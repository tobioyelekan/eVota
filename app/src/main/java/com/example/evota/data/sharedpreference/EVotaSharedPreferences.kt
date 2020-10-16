package com.example.evota.data.sharedpreference

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EVotaSharedPreferences @Inject constructor(
    @ApplicationContext context: Context
) : Preferences {
    override fun setUsername(name: String) {
        preferences.edit().putString(usernameKey, name).apply()
    }

    override fun getUsername(): String {
        return preferences.getString(usernameKey, "")!!
    }

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

    override fun setPollingUnit(pollingUnit: String) {
        preferences.edit().putString(pollingUnitKey, pollingUnit).apply()
    }

    override fun getPollingUnit(): String {
        return preferences.getString(pollingUnitKey, "")!!
    }

    override fun setWard(ward: String) {
        preferences.edit().putString(wardKey, ward).apply()
    }

    override fun getWard(): String {
        return preferences.getString(wardKey, "")!!
    }

    override fun getLga(): String {
        return preferences.getString(lgaKey, "")!!
    }

    override fun setLga(lga: String) {
        preferences.edit().putString(lgaKey, lga).apply()
    }

    override fun setState(state: String) {
        preferences.edit().putString(stateKey, state).apply()
    }

    override fun getState(): String {
        return preferences.getString(stateKey, "")!!
    }

    override fun setData(date: String) {
        preferences.edit().putString(dateKey, date).apply()
    }

    override fun getDate(): String {
        return preferences.getString(dateKey, "")!!
    }

    override fun getTime(): String {
        return preferences.getString(timeKey, "")!!
    }

    override fun setTime(time: String) {
        preferences.edit().putString(timeKey, time).apply()
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
        private const val pollingUnitKey = "com.example.evota.constants.constants.pollingUnitKey"
        private const val wardKey = "com.example.evota.constants.constants.ward"
        private const val lgaKey = "com.example.evota.constants.constants.lga"
        private const val stateKey = "com.example.evota.constants.constants.state"
        private const val dateKey = "com.example.evota.constants.constants.date"
        private const val timeKey = "com.example.evota.constants.constants.time"
        private const val nameKey = "com.example.evota.constants.constants.name"
        private const val imgKey = "com.example.evota.constants.constants.img"
    }
}