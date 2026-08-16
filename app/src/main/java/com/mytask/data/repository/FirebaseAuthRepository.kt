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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }

        auth.addAuthStateListener(listener)

        awaitClose {
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
            val authResult = auth
                .createUserWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val user = authResult.user
                ?: error("Akun Firebase tidak berhasil dibuat.")

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

            // Authentication harus tetap berhasil walaupun Firestore belum siap.
            runCatching {
                saveCloudProfile(user, profile)
            }

            userProfileRepository.saveProfile(
                uid = user.uid,
                name = profile.name,
                program = profile.program
            )

            Result.success(profile)
        } catch (error: Throwable) {
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<UserProfile> {
        return try {
            val authResult = auth
                .signInWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val user = authResult.user
                ?: error("Akun tidak ditemukan.")

            // Firestore bukan syarat agar user dapat masuk.
            val profile = runCatching {
                loadCloudProfile(user)
            }.getOrElse {
                cachedOrFirebaseProfile(user)
            }

            userProfileRepository.saveProfile(
                uid = user.uid,
                name = profile.name,
                program = profile.program
            )

            Result.success(profile)
        } catch (error: Throwable) {
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun signInWithGoogle(
        context: Context
    ): Result<UserProfile> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(
                    context.getString(
                        com.mytask.R.string.default_web_client_id
                    )
                )
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context,
                request
            )

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

            val existingProfile = runCatching {
                loadCloudProfile(user)
            }.getOrNull()

            val profile = existingProfile
                ?: run {
                    val localFallback = runCatching {
                        userProfileRepository.profile.first()
                    }.getOrNull()

                    localFallback?.takeIf {
                        userProfileRepository.uid.first() == user.uid
                    } ?: UserProfile(
                        name = user.displayName
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: "Mahasiswa",
                        program = "Program Studi belum diatur"
                    )
                }

            if (existingProfile == null) {
                runCatching {
                    saveCloudProfile(user, profile)
                }
            }

            userProfileRepository.saveProfile(
                uid = user.uid,
                name = profile.name,
                program = profile.program
            )

            Result.success(profile)
        } catch (error: Throwable) {
            auth.signOut()
            userProfileRepository.clearProfile()
            Result.failure(error)
        }
    }

    suspend fun continueAsGuest(
        name: String,
        program: String
    ): Result<UserProfile> {
        return runCatching {
            val profile = UserProfile(
                name = name.trim(),
                program = program.trim()
            )

            if (profile.name.isBlank() || profile.program.isBlank()) {
                error("Nama dan program studi wajib diisi.")
            }

            auth.signOut()

            userProfileRepository.saveGuestProfile(
                name = profile.name,
                program = profile.program
            )

            profile
        }
    }

    suspend fun reloadProfile(): Result<UserProfile> {
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

            userProfileRepository.saveProfile(
                uid = refreshedUser.uid,
                name = profile.name,
                program = profile.program
            )

            Result.success(profile)
        } catch (error: Throwable) {
            val cached = userProfileRepository.profile.first()

            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(error)
            }
        }
    }

    suspend fun clearLocalSession() {
        userProfileRepository.clearProfile()
        auth.signOut()
    }

    fun signOut() {
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
            return cachedProfile
        }

        return UserProfile(
            name = user.displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Mahasiswa",
            program = "Program Studi belum diatur"
        )
    }
}
