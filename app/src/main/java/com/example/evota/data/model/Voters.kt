package com.example.evota.data.model

data class VoterData(
    val id: String,
    val name: String,
    val identification: String
)

data class VoterSearchResponse(
    val results: List<VoterData>
)