package com.iiab.networkinspector.di

import androidx.room.Room
import com.iiab.networkinspector.data.NetworkInspectorDatabase
import com.iiab.networkinspector.data.NetworkInspectorRepositoryImpl
import com.iiab.networkinspector.domain.ClearNetworkCallsUseCase
import com.iiab.networkinspector.domain.GetNetworkCallDetailsUseCase
import com.iiab.networkinspector.domain.NetworkInspectorRepository
import com.iiab.networkinspector.domain.ObserveNetworkCallsUseCase
import com.iiab.networkinspector.interceptor.DebugNetworkInspectorInterceptor
import com.iiab.networkinspector.notification.NetworkInspectorNotifier
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

