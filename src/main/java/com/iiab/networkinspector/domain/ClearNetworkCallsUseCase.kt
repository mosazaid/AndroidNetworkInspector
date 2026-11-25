package com.iiab.networkinspector.domain

class ClearNetworkCallsUseCase(
    private val repository: NetworkInspectorRepository
) {
    suspend operator fun invoke() {
        repository.clearAll()
    }
}

