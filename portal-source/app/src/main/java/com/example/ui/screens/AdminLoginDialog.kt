package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import com.example.data.model.AppLanguage
import com.example.util.AuthManager
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AdminLoginDialog(
    language: AppLanguage,
    onVerifyAdminRole: ((isAdmin: Boolean, role: String?) -> Unit) -> Unit,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun verifyAndProceed() {
        onVerifyAdminRole { isAdmin, _ ->
            isLoading = false
            if (isAdmin) {
                onSuccess()
            } else {
                try {
                    FirebaseAuth.getInstance().signOut()
                } catch (_: Throwable) {
                }
                errorMessage = if (language == AppLanguage.BN) {
                    "অননুমোদিত অ্যাকাউন্ট: আপনার এডমিন অনুমতি নেই।"
                } else {
                    "Access denied: This account does not have administrator privileges."
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("admin_login_dialog")
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF006A4E),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (language == AppLanguage.BN) "এডমিন লগইন" else "Admin Login",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006A4E)
                )
                Text(
                    text = if (language == AppLanguage.BN)
                        "অনুমোদিত এডমিন অ্যাকাউন্ট দিয়ে প্রবেশ করুন"
                    else
                        "Sign in with an authorized administrator account",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text(if (language == AppLanguage.BN) "এডমিন ইমেল" else "Admin Email") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF006A4E),
                        focusedLabelColor = Color(0xFF006A4E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_email_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text(if (language == AppLanguage.BN) "পাসওয়ার্ড" else "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF006A4E),
                        focusedLabelColor = Color(0xFF006A4E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        if (trimmedEmail.isBlank() || password.isBlank()) {
                            errorMessage = if (language == AppLanguage.BN) {
                                "দয়া করে ইমেল এবং পাসওয়ার্ড দিন।"
                            } else {
                                "Please enter both email and password."
                            }
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(trimmedEmail, password)
                            .addOnSuccessListener {
                                verifyAndProceed()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                val msg = if (language == AppLanguage.BN) {
                                    "লগইন ব্যর্থ হয়েছে: ${e.localizedMessage ?: "তথ্য সঠিক নয়"}"
                                } else {
                                    "Login failed: ${e.localizedMessage ?: "Invalid credentials"}"
                                }
                                errorMessage = msg
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_login_submit_button"),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text(if (language == AppLanguage.BN) "লগইন করুন" else "Login", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        AuthManager.signInWithGoogle(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = { verifyAndProceed() },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_google_signin_button"),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF006A4E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "Google দিয়ে এডমিন সাইন-ইন" else "Admin Sign in with Google",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF006A4E)
                    )
                }
            }
        }
    }
}
