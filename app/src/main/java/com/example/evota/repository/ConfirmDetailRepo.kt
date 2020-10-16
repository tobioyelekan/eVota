package com.example.evota.repository

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.*
import com.example.evota.data.model.DeviceDetails
import com.example.evota.data.model.DeviceDetailsResponse
import com.example.evota.data.model.ElectionData
import com.example.evota.data.model.ElectionDataResponse
import com.example.evota.data.remote.DeviceDetailsService
import com.example.evota.data.remote.ElectionService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfirmDetailRepo @Inject constructor(
    private val deviceDetailsService: DeviceDetailsService,
    private val electionService: ElectionService,
    private val executors: AppExecutors
) {

    fun getElectionData(): LiveData<Resource<List<ElectionData>>> {
        return object :
            NetworkOutBoundResource<ElectionDataResponse, List<ElectionData>>(executors) {
            override fun processResponse(response: ApiSuccessResponse<ElectionDataResponse>): List<ElectionData> {
                return response.body.data
            }

            override fun createCall() = electionService.getElectionData()

        }.asLiveData()
    }

    fun getDeviceData(deviceId: String): LiveData<Resource<DeviceDetails>> {
        return object : NetworkOutBoundResource<DeviceDetailsResponse, DeviceDetails>(executors) {
            override fun processResponse(response: ApiSuccessResponse<DeviceDetailsResponse>): DeviceDetails {
                val data = response.body
                return DeviceDetails(
                    state = data.state.name,
                    lga = data.lga.name,
                    ward = data.ward.name,
                    pollingUnit = data.pollingUnit.name
                )
            }

            override fun createCall() = deviceDetailsService.getDeviceDetails(deviceId)

        }.asLiveData()
    }
}