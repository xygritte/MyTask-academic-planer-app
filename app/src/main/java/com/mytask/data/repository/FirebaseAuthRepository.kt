package com.mytask.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mytask.debug.AuthDebugLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val cloudDataSyncRepository: CloudDataSyncRepository
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            AuthDebugLog.d(
                "AUTH_STATE changed: uid=${AuthDebugLog.uid(user?.uid)} signedIn=${user != null}"
            )
            trySend(user)
        }

        AuthDebugLog.d(
            "AUTH_STATE listener attached: currentUid=${AuthDebugLog.uid(auth.currentUser?.uid)}"
        )

        auth.addAuthStateListener(listener)

        awaitClose {
            AuthDebugLog.d("AUTH_STATE listener detached")
            auth.removeAuthStateListener(listener)
        }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun register(
        name: String,
        program: String,
        email: String,
        password: String
    ): Result<UserProfile> {
        AuthDebugLog.d("REGISTER start")

        return try {
            val authResult = auth
                .createUserWithEmailAndPassword(email.trim(), password)
                .await()

            val user = authResult.user
                ?: error("Akun Firebase tidak berhasil dibuat.")

            AuthDebugLog.d(
                "REGISTER Firebase success: uid=${AuthDebugLog.uid(user.uid)}"
            )

            cloudDataSyncRepository.clearLocalSessionData()
            AuthDebugLog.d("REGISTER local workspace cleared")

            val cleanName = name.trim()
            val cleanProgram = program.trim()

            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
            ).await()

            val profile = UserProfile(
                name = cleanName,
                program = cleanProgram
            )

            // Once Firebase authentication succeeds, committing the local
            // session must survive Compose leaving the LoginScreen.
            withContext(NonCancellable) {
                userProfileRepository.saveProfile(
                    uid = user.uid,
                    name = profile.name,
                    program = profile.program
                )
                userProfileRepository.markCloudRestorePending()
                AuthDebugLog.d(
                    "REGISTER local session committed: uid=${AuthDebugLog.uid(user.uid)} restorePending=true"
                )
            }

            runCatching {
                saveCloudProfile(user, profile)
                AuthDebugLog.d("REGISTER Firestore profile saved")
            }.onFailure {
                AuthDebugLog.e("REGISTER Firestore profile save failed", it)
            }

            Result.success(profile)
        } catch (error: Throwable) {
            if (error is CancellationException) {
                AuthDebugLog.e(
                    "REGISTER coroutine cancelled after Firebase authentication; keeping signed-in session",
                    error
                )
                throw error
            }

            AuthDebugLog.e(
                "REGISTER failed: ${error::class.simpleName}: ${error.message}",
                error
            )
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<UserProfile> {
        AuthDebugLog.d("EMAIL_LOGIN start")

        return try {
            val authResult = auth
                .signInWithEmailAndPassword(email.trim(), password)
                .await()

            val user = authResult.user
                ?: error("Akun tidak ditemukan.")

            AuthDebugLog.d(
                "EMAIL_LOGIN Firebase success: uid=${AuthDebugLog.uid(user.uid)}"
            )

            val immediateProfile = UserProfile(
                name = user.displayName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Mahasiswa",
                program = "Program Studi belum diatur"
            )

            // This write must complete even if LoginScreen is removed after
            // FirebaseAuth emits signedIn=true.
            withContext(NonCancellable) {
                userProfileRepository.saveProfile(
                    uid = user.uid,
                    name = immediateProfile.name,
                    program = immediateProfile.program
                )
                userProfileRepository.markCloudRestorePending()
                AuthDebugLog.d(
                    "EMAIL_LOGIN local session committed: uid=${AuthDebugLog.uid(user.uid)} restorePending=true"
                )
            }

            val profile = runCatching {
                loadCloudProfile(user)
            }.onSuccess {
                AuthDebugLog.d("EMAIL_LOGIN Firestore profile loaded")
            }.onFailure {
                AuthDebugLog.e(
                    "EMAIL_LOGIN Firestore profile load failed; keeping local session",
                    it
                )
            }.getOrElse {
                immediateProfile
            }

            withContext(NonCancellable) {
                userProfileRepository.saveProfile(
                    uid = user.uid,
                    name = profile.name,
                    program = profile.program
                )
            }
            AuthDebugLog.d(
                "EMAIL_LOGIN local profile saved: uid=${AuthDebugLog.uid(user.uid)}"
            )

            Result.success(profile)
        } catch (error: Throwable) {
            if (error is CancellationException) {
                AuthDebugLog.e(
                    "EMAIL_LOGIN coroutine cancelled after Firebase authentication; keeping signed-in session",
                    error
                )
                throw error
            }

            AuthDebugLog.e(
                "EMAIL_LOGIN failed: ${error::class.simpleName}: ${error.message}",
                error
            )
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun signInWithGoogle(
        context: Context
    ): Result<UserProfile> {
        AuthDebugLog.d("GOOGLE_LOGIN start")

        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(
                    context.getString(com.mytask.R.string.default_web_client_id)
                )
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            AuthDebugLog.d("GOOGLE_LOGIN credential received")

            val googleCredential = GoogleIdTokenCredential.createFrom(
                result.credential.data
            )

            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleCredential.idToken,
                null
            )

            val authResult = auth
                .signInWithCredential(firebaseCredential)
                .await()

            val user = authResult.user
                ?: error("Akun Google tidak berhasil masuk.")

            AuthDebugLog.d(
                "GOOGLE_LOGIN Firebase success: uid=${AuthDebugLog.uid(user.uid)}"
            )

            val immediateProfile = UserProfile(
                name = user.displayName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Mahasiswa",
                program = "Program Studi belum diatur"
            )

            // Critical session commit. Use NonCancellable because AuthState
            // may remove LoginScreen from Compose immediately after sign-in.
            withContext(NonCancellable) {
                userProfileRepository.saveProfile(
                    uid = user.uid,
                    name = immediateProfile.name,
                    program = immediateProfile.program
                )
                userProfileRepository.markCloudRestorePending()
                AuthDebugLog.d(
                    "GOOGLE_LOGIN local session committed: uid=${AuthDebugLog.uid(user.uid)} restorePending=true"
                )
            }

            val existingProfile = runCatching {
                loadCloudProfile(user)
            }.onSuccess {
                AuthDebugLog.d("GOOGLE_LOGIN Firestore profile loaded")
            }.onFailure {
                AuthDebugLog.e(
                    "GOOGLE_LOGIN Firestore profile load failed; keeping local session",
                    it
                )
            }.getOrNull()

            val profile = existingProfile ?: immediateProfile

            if (existingProfile == null) {
                runCatching {
                    saveCloudProfile(user, profile)
                    AuthDebugLog.d("GOOGLE_LOGIN new Firestore profile saved")
                }.onFailure {
                    AuthDebugLog.e(
                        "GOOGLE_LOGIN new Firestore profile save failed",
                        it
                    )
                }
            }

            withContext(NonCancellable) {
                userProfileRepository.saveProfile(
                    uid = user.uid,
                    name = profile.name,
                    program = profile.program
                )
            }
            AuthDebugLog.d(
                "GOOGLE_LOGIN local profile saved: uid=${AuthDebugLog.uid(user.uid)}"
            )

            Result.success(profile)
        } catch (error: Throwable) {
            if (error is CancellationException) {
                AuthDebugLog.e(
                    "GOOGLE_LOGIN coroutine cancelled after Firebase authentication; keeping signed-in session",
                    error
                )
                throw error
            }

            AuthDebugLog.e(
                "GOOGLE_LOGIN failed: ${error::class.simpleName}: ${error.message}",
                error
            )
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun continueAsGuest(
        name: String,
        program: String
    ): Result<UserProfile> {
        AuthDebugLog.d("GUEST start")

        return runCatching {
            cloudDataSyncRepository.clearLocalSessionData()
            AuthDebugLog.d("GUEST local workspace cleared")

            val profile = UserProfile(
                name = name.trim(),
                program = program.trim()
            )

            if (profile.name.isBlank() || profile.program.isBlank()) {
                error("Nama dan program studi wajib diisi.")
            }

            auth.signOut()
            AuthDebugLog.d("GUEST Firebase signOut executed")

            withContext(NonCancellable) {
                userProfileRepository.saveGuestProfile(
                    name = profile.name,
                    program = profile.program
                )
                userProfileRepository.clearCloudRestorePending()
            }
            AuthDebugLog.d("GUEST local profile saved; restorePending=false")

            profile
        }.onSuccess {
            AuthDebugLog.d("GUEST success")
        }.onFailure {
            AuthDebugLog.e("GUEST failed", it)
        }
    }

    suspend fun reloadProfile(): Result<UserProfile> {
        AuthDebugLog.d(
            "RELOAD_PROFILE start: uid=${AuthDebugLog.uid(auth.currentUser?.uid)}"
        )

        val user = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("Belum ada pengguna yang login.")
            )

        return try {
            user.reload().await()

            val refreshedUser = auth.currentUser
                ?: error("Sesi login tidak tersedia.")

            val profile = runCatching {
                loadCloudProfile(refreshedUser)
            }.getOrElse {
                cachedOrFirebaseProfile(refreshedUser)
            }

            withContext(NonCancellable) {
                userProfileRepository.saveProfile(
                    uid = refreshedUser.uid,
                    name = profile.name,
                    program = profile.program
                )
            }

            AuthDebugLog.d(
                "RELOAD_PROFILE success: uid=${AuthDebugLog.uid(refreshedUser.uid)}"
            )
            Result.success(profile)
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }

            AuthDebugLog.e("RELOAD_PROFILE failed", error)
            val cached = userProfileRepository.profile.first()

            if (cached != null) {
                AuthDebugLog.d("RELOAD_PROFILE using cached profile")
                Result.success(cached)
            } else {
                Result.failure(error)
            }
        }
    }

    suspend fun clearLocalSession() {
        AuthDebugLog.d(
            "LOGOUT start: currentUid=${AuthDebugLog.uid(auth.currentUser?.uid)}"
        )
        cloudDataSyncRepository.clearLocalSessionData()
        AuthDebugLog.d("LOGOUT local workspace cleared")
        withContext(NonCancellable) {
            userProfileRepository.clearProfile()
        }
        AuthDebugLog.d("LOGOUT local profile cleared")
        auth.signOut()
        AuthDebugLog.d("LOGOUT Firebase signOut executed")
    }

    fun signOut() {
        AuthDebugLog.d(
            "SIGN_OUT direct call: currentUid=${AuthDebugLog.uid(auth.currentUser?.uid)}"
        )
        auth.signOut()
    }

    private suspend fun saveCloudProfile(
        user: FirebaseUser,
        profile: UserProfile
    ) {
        firestore
            .collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "uid" to user.uid,
                    "name" to profile.name,
                    "program" to profile.program,
                    "email" to user.email,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun loadCloudProfile(
        user: FirebaseUser
    ): UserProfile {
        val profileDocument = firestore
            .collection("users")
            .document(user.uid)
            .get()
            .await()

        val name = profileDocument
            .getString("name")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: user.displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: error("Profil akun belum lengkap.")

        val program = profileDocument
            .getString("program")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Program Studi belum diatur"

        return UserProfile(
            name = name,
            program = program
        )
    }

    private suspend fun cachedOrFirebaseProfile(
        user: FirebaseUser
    ): UserProfile {
        val cachedUid = userProfileRepository.uid.first()
        val cachedProfile = userProfileRepository.profile.first()

        if (cachedUid == user.uid && cachedProfile != null) {
            AuthDebugLog.d(
                "PROFILE_FALLBACK using cached profile for uid=${AuthDebugLog.uid(user.uid)}"
            )
            return cachedProfile
        }

        AuthDebugLog.d(
            "PROFILE_FALLBACK using Firebase displayName for uid=${AuthDebugLog.uid(user.uid)}"
        )
        return UserProfile(
            name = user.displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Mahasiswa",
            program = "Program Studi belum diatur"
        )
    }
}
