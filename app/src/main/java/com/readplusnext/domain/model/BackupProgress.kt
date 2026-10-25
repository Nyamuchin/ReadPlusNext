package com.readplusnext.domain.model

data class BackupProgress(
    val running: Boolean = false,
    val phase: String = "",
    val processed: Int = 0,
    val total: Int = 0,
)
