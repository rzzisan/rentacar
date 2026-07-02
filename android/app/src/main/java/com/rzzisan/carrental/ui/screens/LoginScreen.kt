package com.rzzisan.carrental.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.rzzisan.carrental.BuildConfig
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.LoginRequest
import com.rzzisan.carrental.data.auth.AuthTokenStore
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Primary, PrimaryDark))
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    "🚗  CarRental",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    s.login,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )

                // Error
                if (errorMsg.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            errorMsg,
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = "" },
                    label = { Text(s.email) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = "" },
                    label = { Text(s.password) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Remember Me toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(s.rememberMe, fontSize = 13.sp, color = InkMuted)
                }

                // Login button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMsg = "${s.email} ও ${s.password} দিন"
                            return@Button
                        }
                        loading = true
                        errorMsg = ""
                        scope.launch {
                            try {
                                val res = ApiClient.service.login(
                                    LoginRequest(
                                        email = email.trim(),
                                        password = password,
                                        rememberMe = rememberMe
                                    )
                                )
                                if (res.success && res.data != null) {
                                    res.data.token?.let { AuthTokenStore.saveToken(it) }
                                    AuthTokenStore.saveUserInfo(
                                        id = res.data.id,
                                        username = res.data.username,
                                        role = res.data.role,
                                        tenantId = res.data.tenantId
                                    )
                                    onLoginSuccess()
                                } else {
                                    errorMsg = res.message ?: s.loginFailed
                                }
                            } catch (e: HttpException) {
                                Log.e("LoginScreen", "HTTP ${e.code()} error", e)
                                val body = e.response()?.errorBody()?.string()
                                // suspended tenant (403), invalid credentials ইত্যাদির প্রকৃত বার্তা
                                // এখানেই আসে (json_response 4xx status-এ পাঠায়) — DEBUG/release নির্বিশেষে দেখাতে হবে,
                                // নাহলে suspended tenant-এর ইউজার আসল কারণ জানতেই পারবে না production build-এ
                                val serverMsg = body?.let {
                                    try { org.json.JSONObject(it).optString("message").ifBlank { null } }
                                    catch (_: Exception) { null }
                                }
                                errorMsg = serverMsg
                                    ?: if (!body.isNullOrBlank() && BuildConfig.DEBUG) "HTTP ${e.code()}: $body"
                                       else s.loginFailed
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "Login error", e)
                                errorMsg = if (BuildConfig.DEBUG)
                                    "${s.serverError}\n${e.javaClass.simpleName}: ${e.message}"
                                else s.serverError
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        if (loading) s.loggingIn else s.loginBtn,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
