package com.pennywise.app.data.util

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pennywise.app.data.local.PennyWiseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PennyWiseDatabase
) {
    suspend fun backupToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = context.getDatabasePath(PennyWiseDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                error("Database file not found")
            }

            checkpointAndCloseDatabase()

            context.contentResolver.openOutputStream(uri)?.use { output ->
                dbFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open backup destination")

            PennyWiseDatabase.getDatabase(context)
            Unit
        }
    }

    suspend fun restoreFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = context.getDatabasePath(PennyWiseDatabase.DATABASE_NAME)
            dbFile.parentFile?.mkdirs()

            // Checkpoint and close to flush WAL, then release all connections
            checkpointAndCloseDatabase()

            // Delete ALL database files first for a clean slate - prevents mixing old WAL/shm
            // with the restored main db file
            deleteAllDatabaseFiles(dbFile)

            // Copy backup over the (now-deleted) db file path
            context.contentResolver.openInputStream(uri)?.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: error("Unable to open backup source")

            // Ensure no leftover WAL/shm from backup (backup is checkpointed single file)
            deleteSidecarFiles(dbFile)

            PennyWiseDatabase.getDatabase(context)
            Unit
        }
    }

    private fun deleteAllDatabaseFiles(dbFile: File) {
        dbFile.delete()
        deleteSidecarFiles(dbFile)
    }

    private fun checkpointAndCloseDatabase() {
        val sqliteDb: SupportSQLiteDatabase = database.openHelper.writableDatabase
        sqliteDb.query("PRAGMA wal_checkpoint(FULL)").close()
        sqliteDb.close()
        PennyWiseDatabase.closeDatabase()
    }

    private fun deleteSidecarFiles(dbFile: File) {
        File("${dbFile.path}-wal").delete()
        File("${dbFile.path}-shm").delete()
    }
}
