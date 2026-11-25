package com.mousa.networkinspector.di

import androidx.room.Room
import com.mousa.networkinspector.data.NetworkInspectorDatabase
import com.mousa.networkinspector.data.NetworkInspectorRepositoryImpl
import com.mousa.networkinspector.domain.ClearNetworkCallsUseCase
import com.mousa.networkinspector.domain.GetNetworkCallDetailsUseCase
import com.mousa.networkinspector.domain.NetworkInspectorRepository
import com.mousa.networkinspector.domain.ObserveNetworkCallsUseCase
import com.mousa.networkinspector.interceptor.DebugNetworkInspectorInterceptor
import com.mousa.networkinspector.notification.NetworkInspectorNotifier
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object NetworkInspectorModule {

    val modules = module {
        single {
            Room.databaseBuilder(
                androidContext(),
                NetworkInspectorDatabase::class.java,
                "network_inspector_db"
            ).build()
        }

        single { get<NetworkInspectorDatabase>().networkCallDao() }

        single<NetworkInspectorRepository> {
            NetworkInspectorRepositoryImpl(
                dao = get()
            )
        }

        // Use cases
        single { ObserveNetworkCallsUseCase(get()) }
        single { GetNetworkCallDetailsUseCase(get()) }
        single { ClearNetworkCallsUseCase(get()) }

        // Notification helper
        single { NetworkInspectorNotifier(androidContext()) }

        // OkHttp interceptor
        single {
            DebugNetworkInspectorInterceptor(
                repository = get(),
                notifier = get()
            )
        }
    }
}

