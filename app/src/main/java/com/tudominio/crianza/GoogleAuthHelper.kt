package com.tudominio.crianza

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

const val GOOGLE_WEB_CLIENT_ID = "56532389351-tg49bl11sliq31it39i0gmtntn3n510j.apps.googleusercontent.com"

data class UsuarioGoogle(
    val id: String,
    val nombre: String,
    val email: String,
    val fotoUrl: String?
)

class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()
    val configurado: Boolean get() = GOOGLE_WEB_CLIENT_ID != "TU_CLIENT_ID.apps.googleusercontent.com"

    /**
     * Devuelve el usuario Firebase actual si ya tiene sesión activa.
     */
    fun obtenerUsuarioActual(): UsuarioGoogle? {
        val user = firebaseAuth.currentUser ?: return null
        return UsuarioGoogle(
            id = user.uid,
            nombre = user.displayName ?: "",
            email = user.email ?: "",
            fotoUrl = user.photoUrl?.toString()
        )
    }

    suspend fun iniciarSesion(activity: Activity): Result<UsuarioGoogle> {
        if (!configurado) {
            return Result.failure(Exception("Google Sign-In no configurado. Ver GOOGLE_WEB_CLIENT_ID en GoogleAuthHelper.kt"))
        }
        return try {
            // 1. Obtener credencial Google via Credential Manager
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activity, request)
            val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)

            // 2. Autenticar con Firebase usando el ID token
            val idToken = googleCred.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Error al autenticar con Firebase"))

            Result.success(
                UsuarioGoogle(
                    id = firebaseUser.uid,
                    nombre = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    fotoUrl = firebaseUser.photoUrl?.toString()
                )
            )
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Inicio de sesión cancelado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cerrarSesion() {
        try {
            firebaseAuth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // ignorar
        }
    }

    companion object {
        fun compartirApp(context: Context, codigofamiliar: String) {
            val texto = """
¡Te invito a usar Crianza! 📱

Es una app para coordinar la crianza de los hijos entre padres.
Descargala acá: https://github.com/Ostro1982/crianza-app/releases/latest

Código de familia para conectarnos: $codigofamiliar
            """.trimIndent()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, texto)
            }
            context.startActivity(Intent.createChooser(intent, "Invitar co-padre/co-madre"))
        }
    }
}
