# NetworkInspector

[![](https://jitpack.io/v/mosazaid/NetworkInspector.svg)](https://jitpack.io/#mosazaid/NetworkInspector)

A plug-and-play Android network debugging library.  
It captures **every OkHttp request/response** (including raw HTML error bodies and converter failures) in a persistent Room database and surfaces them in a beautiful Compose UI accessible via a floating bubble — with copy-to-clipboard support.

---

## Features

- 🔍 Logs all HTTP calls (request + response headers & body)
- 🛡️ Catches **outer errors** before OkHttp (e.g. `EmptyTokenException`, `HtmlResponseException`) via `outerInterceptor`
- 🔄 Captures **Retrofit converter failures** (non-JSON / malformed responses) via `NetworkInspectorConverterFactory`
- 📋 Copy body / error text to clipboard
- 💾 Persistent storage with Room
- 💉 Koin DI — no boilerplate setup required
- 🎨 Built with Jetpack Compose

---

## Setup

### 1. Add JitPack to your `settings.gradle`

```groovy
dependencyResolutionManagement {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. Add the dependency

```groovy
// app/build.gradle or the module that uses it
debugImplementation 'com.github.mosazaid:NetworkInspector:1.0.0'
```

> Tip: use `debugImplementation` so the inspector is only included in debug builds.

---

## Usage

### OkHttp wiring

```kotlin
// In your OkHttpClient builder (Koin / Hilt / manual DI)
val networkInspector: DebugNetworkInspectorInterceptor = get()

val client = OkHttpClient.Builder()
    .apply { networkInspector.attachTo(this) } // ← one line, positions interceptors correctly
    // ... your other interceptors ...
    .build()
```

### Retrofit converter wiring

```kotlin
import com.iiab.networkinspector.retrofit.withNetworkInspector

Retrofit.Builder()
    .addConverterFactory(
        GsonConverterFactory.create(gson).withNetworkInspector(networkInspector)
    )
    .build()
```

### Show the inspector bubble

The inspector registers itself via Koin; a floating bubble appears automatically in debug builds once you call `startKoin` with the `networkInspectorModule`.

---

## License

Apache 2.0
