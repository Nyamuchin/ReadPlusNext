package com.readplusnext.data.source

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** Small SAF helpers shared by the importers. */
object UriUtils {

    /** Provider supplied display name, e.g. `vol_03.cbz`. */
    fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                        cursor.getString(index)
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }

    /** Provider supplied size in bytes, or null when unknown. */
    fun size(context: Context, uri: Uri): Long? {
        if (uri.scheme == "file") return uri.path?.let { java.io.File(it).length() }
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                        cursor.getLong(index)
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }
}
