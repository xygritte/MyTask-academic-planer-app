package com.mytask.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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
        return try {
            AuthDebugLog.d("REGISTER start")
            val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
                ?: error("Akun Firebase tidak berhasil dibuat.")
            cloudDataSyncRepository.clearLocalSessionData()

            val profile = UserProfile(name.trim(), program.trim())
            if (profile.name.isBlank() || profile.program.isBlank()) {
                error("Nama dan program studi wajib diisi.")
            }

            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(profile.name).build()
            ).await()

            withContext(NonCancellable) {
                userProfileRepository.saveAuthenticatedSession(
                    uid = user.uid,
                    name = profile.name,
                    program = profile.program
                )
                runCatching { saveCloudProfile(user, profile) }
                    .onFailure { AuthDebugLog.e("REGISTER Firestore profile save failed", it) }
            }
            Result.success(profile)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is FirebaseAuthUserCollisionException ||
                error.message?.contains("already in use", ignoreCase = true) == true
            ) {
                auth.signOut()
                userProfileRepository.clearProfile()
                return Result.failure(IllegalStateException("EMAIL_ALREADY_REGISTERED"))
            }
            AuthDebugLog.e("REGISTER failed: ${error::class.simpleName}: ${error.message}", error)
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        return try {
            AuthDebugLog.d("EMAIL_LOGIN start")
            val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
                ?: error("Akun tidak ditemukan.")

            val fallback = firebaseFallbackProfile(user)

            // Firebase Auth state changes can cancel the Compose caller. Profile restore must
            // survive that cancellation so a valid cloud program is not replaced by the default.
            val profile = withContext(NonCancellable) {
                userProfileRepository.saveAuthenticatedSession(user.uid, fallback.name, fallback.program)
                try {
                    loadCloudProfile(user).also {
                        AuthDebugLog.d("EMAIL_LOGIN Firestore profile loaded")
                    }
                } catch (error: Throwable) {
                    AuthDebugLog.e("EMAIL_LOGIN Firestore profile load failed; using fallback", error)
                    fallback
                }
            }

            withContext(NonCancellable) {
                userProfileRepository.saveProfile(user.uid, profile.name, profile.program)
            }
            AuthDebugLog.d(
                "EMAIL_LOGIN local profile saved: uid=${AuthDebugLog.uid(user.uid)} programPresent=${profile.program.isNotBlank()}"
            )
            Result.success(profile)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            AuthDebugLog.e("EMAIL_LOGIN failed: ${error::class.simpleName}: ${error.message}", error)
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<UserProfile> {
        return try {
            AuthDebugLog.d("GOOGLE_LOGIN start")
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(com.mytask.R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            AuthDebugLog.d("GOOGLE_LOGIN credential received")
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            val user = auth.signInWithCredential(firebaseCredential).await().user
                ?: error("Akun Google tidak berhasil masuk.")

            AuthDebugLog.d("GOOGLE_LOGIN Firebase success: uid=${AuthDebugLog.uid(user.uid)}")
            val fallback = firebaseFallbackProfile(user)

            val profile = withContext(NonCancellable) {
                userProfileRepository.saveAuthenticatedSession(user.uid, fallback.name, fallback.program)
                try {
                    loadCloudProfile(user).also {
                        AuthDebugLog.d("GOOGLE_LOGIN Firestore profile loaded")
                    }
                } catch (error: Throwable) {
                    AuthDebugLog.e("GOOGLE_LOGIN Firestore profile load failed; using fallback", error)
                    // Only create a cloud profile when the account truly has no profile document.
                    // A cancelled composition must never overwrite an existing cloud program.
                    val existing = runCatching { firestore.collection("users").document(user.uid).get().await() }
                        .getOrNull()
                    if (existing?.exists() == true) {
                        fallback
                    } else {
                        runCatching {
                            saveCloudProfile(user, fallback)
                            AuthDebugLog.d("GOOGLE_LOGIN new Firestore profile saved")
                        }.onFailure {
                            AuthDebugLog.e("GOOGLE_LOGIN new Firestore profile save failed", it)
                        }
                        fallback
                    }
                }
            }

            withContext(NonCancellable) {
                userProfileRepository.saveProfile(user.uid, profile.name, profile.program)
            }
            AuthDebugLog.d(
                "GOOGLE_LOGIN local profile saved: uid=${AuthDebugLog.uid(user.uid)} programPresent=${profile.program.isNotBlank()}"
            )
            Result.success(profile)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            AuthDebugLog.e("GOOGLE_LOGIN failed: ${error::class.simpleName}: ${error.message}", error)
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun continueAsGuest(name: String, program: String): Result<UserProfile> = runCatching {
        cloudDataSyncRepository.clearLocalSessionData()
        val profile = UserProfile(name.trim(), program.trim())
        if (profile.name.isBlank() || profile.program.isBlank()) {
            error("Nama dan program studi wajib diisi.")
        }
        auth.signOut()
        withContext(NonCancellable) {
            userProfileRepository.saveGuestProfile(profile.name, profile.program)
            userProfileRepository.clearCloudRestorePending()
        }
        profile
    }

    suspend fun reloadProfile(): Result<UserProfile> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Belum ada pengguna yang login."))
        return try {
            val profile = withContext(NonCancellable) {
                runCatching { loadCloudProfile(user) }
                    .getOrElse { cachedOrFirebaseProfile(user) }
            }
            withContext(NonCancellable) {
                userProfileRepository.saveProfile(user.uid, profile.name, profile.program)
            }
            Result.success(profile)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Result.failure(error)
        }
    }

    suspend fun updateCurrentUserProfile(name: String, program: String): Result<UserProfile> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Belum ada pengguna yang login."))
        val cleanName = name.trim()
        val cleanProgram = program.trim()
        if (cleanName.isBlank() || cleanProgram.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama dan program studi wajib diisi."))
        }
        return try {
            withContext(NonCancellable) {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(cleanName).build()
                ).await()
                saveCloudProfile(user, UserProfile(cleanName, cleanProgram))
                userProfileRepository.saveProfile(user.uid, cleanName, cleanProgram)
            }
            AuthDebugLog.d(
                "PROFILE_UPDATE online success: uid=${AuthDebugLog.uid(user.uid)} nameLength=${cleanName.length} programLength=${cleanProgram.length}"
            )
            Result.success(UserProfile(cleanName, cleanProgram))
        } catch (error: Throwable) {
            AuthDebugLog.e("PROFILE_UPDATE online failed", error)
            Result.failure(error)
        }
    }

    suspend fun clearLocalSession() {
        AuthDebugLog.d("LOGOUT start: currentUid=${AuthDebugLog.uid(auth.currentUser?.uid)}")
        cloudDataSyncRepository.clearLocalSessionData()
        withContext(NonCancellable) { userProfileRepository.clearProfile() }
        auth.signOut()
        AuthDebugLog.d("LOGOUT Firebase signOut executed")
    }

    fun signOut() {
        auth.signOut()
    }

    private fun firebaseFallbackProfile(user: FirebaseUser): UserProfile = UserProfile(
        name = user.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "Mahasiswa",
        program = "Program Studi belum diatur"
    )

    private suspend fun saveCloudProfile(user: FirebaseUser, profile: UserProfile) {
        firestore.collection("users").document(user.uid).set(
            mapOf(
                "uid" to user.uid,
                "name" to profile.name,
                "program" to profile.program,
                "email" to user.email,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun loadCloudProfile(user: FirebaseUser): UserProfile {
        val document = firestore.collection("users").document(user.uid).get().await()
        val name = document.getString("name")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: user.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: error("Profil akun belum lengkap.")
        val program = document.getString("program")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Program Studi belum diatur"
        return UserProfile(name, program)
    }

    private suspend fun cachedOrFirebaseProfile(user: FirebaseUser): UserProfile {
        val cachedUid = userProfileRepository.uid.first()
        val cached = userProfileRepository.profile.first()
        if (cachedUid == user.uid && cached != null) return cached
        return firebaseFallbackProfile(user)
    }
}
