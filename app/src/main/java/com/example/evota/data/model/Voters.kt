package com.example.evota.data.model

import com.google.gson.annotations.SerializedName

data class VoterData(
    val id: String,
    val name: String,
    val identification: String
)

data class VoterSearchResponse(
    val results: List<VoterData>
)

data class VoteSuccessResponse(
    val message: String,
    @SerializedName("voter")
    val voterInfo: VoteInfo
)

data class VoteInfo(
    val id: String,
    val name: String,
    val date: String,
    val time: String
)