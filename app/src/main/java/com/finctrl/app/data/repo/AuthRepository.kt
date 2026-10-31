package com.finctrl.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {
    val isSignedIn: Boolean get() = auth.currentUser != null
    val uid: String? get() = auth.currentUser?.uid
    val email: String? get() = auth.currentUser?.email

    suspend fun signIn(email: String, password: String) { auth.signInWithEmailAndPassword(email, password).await() }
    suspend fun register(email: String, password: String) { auth.createUserWithEmailAndPassword(email, password).await() }
    fun signOut() = auth.signOut()
}
