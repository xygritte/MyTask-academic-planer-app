package com.mytask.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance()

    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()

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

            val authResult =
                auth.createUserWithEmailAndPassword(
                    email.trim(),
                    password
                ).await()

            val user =
                authResult.user
                    ?: error("Akun Firebase tidak berhasil dibuat.")

            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
            ).await()

            val profile = UserProfile(
                name = name.trim(),
                program = program.trim()
            )

            firestore
                .collection("users")
                .document(user.uid)
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "name" to profile.name,
                        "program" to profile.program,
                        "email" to user.email,
                        "createdAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()

            userProfileRepository.saveProfile(
                uid = user.uid,
                name = profile.name,
                program = profile.program
            )

            Result.success(profile)

        } catch (error: Throwable) {

            runCatching {
                auth.currentUser?.delete()?.await()
            }

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

            val authResult =
                auth.signInWithEmailAndPassword(
                    email.trim(),
                    password
                ).await()

            val user =
                authResult.user
                    ?: error("Akun tidak ditemukan.")

            val profileDocument =
                firestore
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .await()

            val name =
                profileDocument
                    .getString("name")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: user.displayName
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                    ?: error("Profil akun belum lengkap.")

            val program =
                profileDocument
                    .getString("program")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: error("Program studi pada profil belum diatur.")

            userProfileRepository.saveProfile(
                uid = user.uid,
                name = name,
                program = program
            )

            Result.success(
                UserProfile(
                    name = name,
                    program = program
                )
            )

        } catch (error: Throwable) {

            auth.signOut()
            userProfileRepository.clearProfile()

            Result.failure(error)
        }
    }

    suspend fun reloadProfile(): Result<UserProfile> {

        val user =
            auth.currentUser
                ?: return Result.failure(
                    IllegalStateException(
                        "Belum ada pengguna yang login."
                    )
                )

        return try {

            user.reload().await()

            val refreshedUser =
                auth.currentUser
                    ?: error("Sesi login tidak tersedia.")

            val profileDocument =
                firestore
                    .collection("users")
                    .document(refreshedUser.uid)
                    .get()
                    .await()

            val name =
                profileDocument
                    .getString("name")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: refreshedUser.displayName
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                    ?: error("Profil akun belum lengkap.")

            val program =
                profileDocument
                    .getString("program")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: error("Program studi pada profil belum diatur.")

            val profile =
                UserProfile(
                    name = name,
                    program = program
                )

            userProfileRepository.saveProfile(
                uid = refreshedUser.uid,
                name = profile.name,
                program = profile.program
            )

            Result.success(profile)

        } catch (error: Throwable) {

            val cached =
                userProfileRepository.profile.first()

            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(error)
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
