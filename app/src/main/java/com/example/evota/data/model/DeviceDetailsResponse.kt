package com.example.evota.data.model

import com.google.gson.annotations.SerializedName

data class DeviceDetailsResponse(
    val state: DeviceDataDetails,
    val lga: DeviceDataDetails,
    val ward: DeviceDataDetails,
    @SerializedName("polling_unit")
    val pollingUnit: DeviceDataDetails
)

data class DeviceDataDetails(
    val id: String,
    val name: String,
    val code: String,
    val createdAt: String,
    val updated_at: String
)

data class DeviceDetails(
    val state: String,
    val lga: String,
    val ward: String,
    val pollingUnit: String
)