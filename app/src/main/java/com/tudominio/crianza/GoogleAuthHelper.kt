package com.tudominio.crianza

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * IMPORTANTE: Para habilitar Google Sign-In, seguí estos pasos:
 * 1. Crear proyecto en console.cloud.google.com
 * 2. Activar la API "Google Identity"
 * 3. Crear credencial OAuth 2.0 → tipo "Aplicación Web"
 * 4. Copiar el "Client ID" y reemplazar el valor de GOOGLE_WEB_CLIENT_ID abajo.
 * 5. También crear credencial tipo "Android" con tu SHA-1 (ver: Build > Generate Signed Bundle > SHA-1)
 */
const val GOOGLE_WEB_CLIENT_ID = "TU_CLIENT_ID.apps.googleusercontent.com"

data class UsuarioGoogle(
    val id: String,
    val nombre: String,
    val email: String,
    val fotoUrl: String?
)

class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    val configurado: Boolean get() = GOOGLE_WEB_CLIENT_ID != "TU_CLIENT_ID.apps.googleusercontent.com"

    suspend fun iniciarSesion(activity: Activity): Result<UsuarioGoogle> {
        if (!configurado) {
            return Result.failure(Exception("Google Sign-In no configurado. Ver GOOGLE_WEB_CLIENT_ID en GoogleAuthHelper.kt"))
        }
        return try {
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
            Result.success(
                UsuarioGoogle(
                    id = googleCred.id,
                    nombre = googleCred.displayName ?: "",
                    email = googleCred.id,
                    fotoUrl = googleCred.profilePictureUri?.toString()
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
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // ignorar
        }
    }

    companion object {
        fun compartirApp(context: Context, codigofamiliar: String) {
            val texto = """
¡Te invito a usar Crianza Compartida! 📱

Es una app para coordinar la crianza de los hijos entre padres.
Podés descargarla en Google Play buscando "Crianza Compartida".

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
