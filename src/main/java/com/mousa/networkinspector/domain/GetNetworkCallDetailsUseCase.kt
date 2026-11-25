package com.mousa.networkinspector.domain

import kotlinx.coroutines.flow.Flow

class GetNetworkCallDetailsUseCase(
    private val repository: NetworkInspectorRepository
) {
    operator fun invoke(id: Long): Flow<NetworkCallDetails?> {
        return repository.observeCallDetails(id)
    }
}

