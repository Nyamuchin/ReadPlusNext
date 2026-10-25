package com.readplusnext.core.util

/** Lower case extension without the dot, or an empty string when there is none. */
fun String.extensionOf(): String = substringAfterLast('.', "").lowercase()

/** Drops the last extension, keeping names such as `.hidden` intact. */
fun String.withoutExtension(): String {
    val dot = lastIndexOf('.')
    return if (dot > 0) substring(0, dot) else this
}

/** Removes characters that are not safe inside a file name. */
fun String.toSafeFileName(fallback: String = "page"): String {
    val cleaned = buildString {
        for (char in this@toSafeFileName) {
            append(if (char.isLetterOrDigit() || char == '.' || char == '_' || char == '-') char else '_')
        }
    }.trim('_', '.')
    return cleaned.ifEmpty { fallback }
}
