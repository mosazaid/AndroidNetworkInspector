package com.iiab.networkinspector

import androidx.core.content.FileProvider

/**
 * A dedicated FileProvider subclass for the network-inspector module.
 *
 * Library modules cannot reuse the host app's `androidx.core.content.FileProvider`
 * directly because Android forbids two <provider> entries sharing the same class name
 * in the merged manifest. Using a subclass gives this module its own unique class
 * while still getting all FileProvider functionality.
 */
class NetworkInspectorFileProvider : FileProvider()
