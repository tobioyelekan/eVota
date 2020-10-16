package com.example.evota.data.sharedpreference

interface Preferences {
    fun isUserLoggedIn(): Boolean
    fun setImg(img: String)
    fun getImgUrl(): String
    fun setName(name: String)
    fun getName(): String
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
    fun setPollingUnit(pollingUnit: String)
    fun getPollingUnit(): String
    fun setWard(ward: String)
    fun getWard(): String
    fun getLga(): String
    fun setLga(lga: String)
    fun setState(state: String)
    fun getState(): String
    fun setData(date: String)
    fun getDate(): String
    fun getTime(): String
    fun setTime(time: String)
}