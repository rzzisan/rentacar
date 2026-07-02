package com.rzzisan.carrental.data.auth

import android.content.Context

class AuthStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("auth_store", Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString("token", null)
    fun saveToken(token: String) = prefs.edit().putString("token", token).apply()
    fun getRole(): String? = prefs.getString("role", null)
    fun getUsername(): String? = prefs.getString("username", null)
    fun getUserId(): Int = prefs.getInt("user_id", 0)
    // superadmin-এর tenant_id নেই — SharedPreferences nullable Int সাপোর্ট করে না, তাই -1 sentinel দিয়ে "নেই" বোঝানো হয়
    fun getTenantId(): Int? = prefs.getInt("tenant_id", -1).takeIf { it != -1 }

    fun saveUserInfo(id: Int, username: String, role: String, tenantId: Int? = null) = prefs.edit()
        .putInt("user_id", id)
        .putString("username", username)
        .putString("role", role)
        .putInt("tenant_id", tenantId ?: -1)
        .apply()

    fun clear() = prefs.edit().clear().apply()
}

object AuthTokenStore {
    private val storage by lazy {
        AuthStorage(com.rzzisan.carrental.AppContext.app)
    }

    fun getToken(): String? = storage.getToken()
    fun saveToken(token: String) = storage.saveToken(token)
    fun getRole(): String? = storage.getRole()
    fun getUsername(): String? = storage.getUsername()
    fun getUserId(): Int = storage.getUserId()
    fun getTenantId(): Int? = storage.getTenantId()
    fun saveUserInfo(id: Int, username: String, role: String, tenantId: Int? = null) =
        storage.saveUserInfo(id, username, role, tenantId)
    fun clear() = storage.clear()
}
