package com.example.evota.data.sharedpreference

interface Preferences {
    fun isUserLoggedIn(): Boolean
    fun setImg(img: String)
    fun getImgUrl(): String
    fun setName(name: String)
    fun getName(): String
    fun getUserId(): String
    fun setUserId(userId: String)
    fun getToken(): String
    fun setToken(token: String)
    fun setEmail(email: String)
    fun getEmail(): String
    fun setPhone(phone: String)
    fun getPhone(): String
    fun setPollingUnit(polling: String)
    fun getPollingUnit(): String
}