package com.rzzisan.carrental.util

import retrofit2.HttpException

/**
 * ব্যাকএন্ডের JSON error body (json_response()-এর `message` ফিল্ড) থেকে প্রকৃত কারণ বের করে —
 * suspended tenant / validation error ইত্যাদির আসল বার্তা ইউজারকে দেখানোর জন্য। DEBUG/release
 * নির্বিশেষে ব্যবহার করা উচিত (এই প্যাটার্ন প্রথম LoginScreen.kt-এ ঠিক করা হয়েছিল; require_active_tenant()
 * প্রতিটি authenticated রিকোয়েস্টে চেক হয় বলে যেকোনো স্ক্রিনেই মাঝ-সেশনে suspended-tenant 403 আসতে পারে)।
 */
fun errorMessageOf(e: Throwable, fallback: String): String {
    if (e is HttpException) {
        val body = e.response()?.errorBody()?.string()
        val serverMsg = body?.let {
            try { org.json.JSONObject(it).optString("message").ifBlank { null } }
            catch (_: Exception) { null }
        }
        if (serverMsg != null) return serverMsg
    }
    return fallback
}
