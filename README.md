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
debugImplementation 'com.github.mosazaid:NetworkInspector:1.0.1'
```
Open https://jitpack.io/#mosazaid/AndroidNetworkInspector
Click the Releases tab, find 1.0.1 and click Get it.
Wait 1–2 minutes until the log icon turns green. A red icon means the build failed; click it to read the log.

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
import com.mousa.networkinspector.retrofit.withNetworkInspector

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



it was giving this error in sync
Build file '/Users/1802.mosa/StudioProjects/NetworkInspector/build.gradle' line: 2

Plugin [id: 'com.android.library'] was not found in any of the following sources:

- Gradle Core Plugins (plugin is not in 'org.gradle' namespace)
- Included Builds (No included builds contain this plugin)
- Plugin Repositories (plugin dependency must include a version number for this source)


because 
- Root cause: A standalone Android library project requires plugin versions declared in pluginManagement.plugins {} inside settings.gradle. In the main mousa-android-copy project, AGP version was declared in a root buildscript { classpath ... } block — that's not available in the standalone repo.

Fix:

File	Change
settings.gradle	Added pluginManagement { plugins { ... } } with all 4 plugin versions
build.gradle	Removed inline version "..." from plugins {} block (versions now come from settings.gradle)