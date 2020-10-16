package com.example.evota.data.sharedpreference

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EVotaSharedPreferences @Inject constructor(
    @ApplicationContext context: Context
) : Preferences {

    override fun setEmail(email: String) {
        preferences.edit().putString(emailKey, email).apply()
    }

    override fun getEmail(): String {
        return preferences.getString(emailKey, "")!!
    }

    override fun setPhone(phone: String) {
        preferences.edit().putString(phoneKey, phone).apply()
    }

    override fun getPhone(): String {
        return preferences.getString(phoneKey, "")!!
    }

    private val preferences = context.getSharedPreferences("zoneplayer", Context.MODE_PRIVATE)

    override fun isUserLoggedIn(): Boolean = getUserId() != "-1L"

    override fun setImg(img: String) {
        preferences.edit().putString(imgKey, img).apply()
    }

    override fun getImgUrl(): String {
        return preferences.getString(imgKey, "")!!
    }

    override fun setName(name: String) {
        preferences.edit().putString(nameKey, name).apply()
    }

    override fun getName(): String {
        return preferences.getString(nameKey, "")!!
    }

    override fun getUserId(): String {
        return preferences.getString(userIdKey, "-1L")!!
    }

    override fun setUserId(userId: String) {
        preferences.edit().putString(userIdKey, userId).apply()
    }

    override fun getToken(): String {
        return preferences.getString(tokenKey, "")!!
    }

    override fun setToken(token: String) {
        preferences.edit().putString(tokenKey, token).apply()
    }

    companion object {
        private const val userIdKey = "com.example.evota.constants.userId"
        private const val emailKey = "com.example.evota.constants.emailId"
        private const val phoneKey = "com.example.evota.constants.constants.phoneId"
        private const val tokenKey = "com.example.evota.constants.constants.tokenId"
        private const val usernameKey = "com.example.evota.constants.constants.usernameKey"
        private const val nameKey = "com.example.evota.constants.constants.name"
        private const val imgKey = "com.example.evota.constants.constants.img"
    }
}