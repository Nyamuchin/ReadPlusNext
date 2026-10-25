package com.readplusnext.core.util

import java.security.MessageDigest

/** Stable 32 character MD5 hex string used for cache directory keys. */
fun String.md5Hex(): String = toByteArray(Charsets.UTF_8).md5Hex()

fun ByteArray.md5Hex(): String {
    val digest = MessageDigest.getInstance("MD5").digest(this)
    val builder = StringBuilder(digest.size * 2)
    for (byte in digest) {
        builder.append(HEX[(byte.toInt() shr 4) and 0x0F])
        builder.append(HEX[byte.toInt() and 0x0F])
    }
    return builder.toString()
}

private val HEX = "0123456789abcdef".toCharArray()
