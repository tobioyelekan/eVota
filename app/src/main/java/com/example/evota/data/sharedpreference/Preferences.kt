package com.example.evota.data.sharedpreference

interface Preferences {
    fun isUserLoggedIn(): Boolean
    fun getUserId(): String
    fun setUserId(userId: String)
    fun setUsername(name: String)
    fun getUsername(): String
    fun getToken(): String
    fun setToken(token: String)
    fun setEmail(email: String)
    fun getEmail(): String
    fun setPhone(phone: String)
    fun getPhone(): String
}