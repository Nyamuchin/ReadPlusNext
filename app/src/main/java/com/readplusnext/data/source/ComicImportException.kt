package com.readplusnext.data.source

/** Raised when a comic cannot be normalized; the message is shown to the user. */
class ComicImportException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
