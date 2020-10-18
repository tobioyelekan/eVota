package com.example.evota.data.model

data class ElectionDataResponse(
    val data: List<ElectionData>
)

data class ElectionData(
    val id: String,
    val title: String
)

data class VotingData(
    val voter: String,
    val candidate: String,
    val election: String
)