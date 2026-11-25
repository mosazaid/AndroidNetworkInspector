package com.mousa.networkinspector.domain

import kotlinx.coroutines.flow.Flow

class ObserveNetworkCallsUseCase(
    private val repository: NetworkInspectorRepository
) {
    operator fun invoke(limit: Int = 200): Flow<List<NetworkCall>> {
        return repository.observeCalls(limit)
    }
}

