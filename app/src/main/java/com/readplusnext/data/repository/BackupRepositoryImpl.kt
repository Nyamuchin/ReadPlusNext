package com.readplusnext.data.repository

import android.net.Uri
import com.readplusnext.data.backup.BackupManager
import com.readplusnext.domain.model.BackupProgress
import com.readplusnext.domain.repository.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val backupManager: BackupManager,
) : BackupRepository {

    override val progress: StateFlow<BackupProgress> = backupManager.progress

    override suspend fun export(toUri: Uri): Result<Unit> = backupManager.export(toUri)

    override suspend fun import(fromUri: Uri): Result<Unit> = backupManager.import(fromUri)
}
