package com.example.evota.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class CandidateDataResponse(
    val data: List<CandidateData>
)

data class CandidateData(
    val id: String,
    val title: String,
    val candidates: List<Candidate>
)

@Keep
@Parcelize
data class Candidate(
    val electionId: String,
    val electionTitle: String,
    val id: String,
    val name: String,
    @SerializedName("passport")
    val img: String,
    val party: Party,
    val selected: Boolean = false
) : Parcelable

@Keep
@Parcelize
data class Party(
    val id: String,
    val name: String,
    val code: String,
    val logo: String
) : Parcelable