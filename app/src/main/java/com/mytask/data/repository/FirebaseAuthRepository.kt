package com.mytask.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
    ): Result<UserProfile> = runCatching {

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
            name = profile.name,
            program = profile.program
        )

        profile
    }.onFailure {
        // Jika pembuatan profil cloud gagal setelah akun Firebase dibuat,
        // jangan biarkan state registrasi setengah jadi.
        runCatching {
            auth.currentUser?.delete()?.await()
        }
        auth.signOut()
        userProfileRepository.clearProfile()
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<UserProfile> = runCatching {

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
                ?: user.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: "Mahasiswa"

        val program =
            profileDocument
                .getString("program")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Program Studi belum diatur"

        userProfileRepository.saveProfile(
            name = name,
            program = program
        )

        UserProfile(
            name = name,
            program = program
        )
    }.onFailure {
        auth.signOut()
        userProfileRepository.clearProfile()
    }

    suspend fun reloadProfile(): Result<UserProfile> = runCatching {

        val user =
            auth.currentUser
                ?: error("Belum ada pengguna yang login.")

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
                ?: refreshedUser.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: "Mahasiswa"

        val program =
            profileDocument
                .getString("program")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Program Studi belum diatur"

        userProfileRepository.saveProfile(
            name = name,
            program = program
        )

        UserProfile(
            name = name,
            program = program
        )
    }

    fun signOut() {
        auth.signOut()
    }
}
