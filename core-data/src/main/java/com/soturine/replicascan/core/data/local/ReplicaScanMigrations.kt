package com.soturine.replicascan.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE scans ADD COLUMN searchRowId INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE scans SET searchRowId = rowid")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scans_searchRowId ON scans(searchRowId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pages_scanId_pageIndex ON pages(scanId, pageIndex)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS page_ocr_artifacts (
                pageId TEXT NOT NULL PRIMARY KEY,
                rawText TEXT NOT NULL,
                normalizedText TEXT NOT NULL,
                structuredContent TEXT NOT NULL,
                script TEXT NOT NULL,
                engine TEXT NOT NULL,
                engineVersion TEXT NOT NULL,
                pipelineVersion TEXT NOT NULL,
                sourceFingerprint TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(pageId) REFERENCES pages(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS scan_search_fts
            USING FTS4(scanId TEXT NOT NULL, title TEXT NOT NULL, tags TEXT NOT NULL, ocrText TEXT NOT NULL)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO scan_search_fts(rowid, scanId, title, tags, ocrText)
            SELECT scans.searchRowId, scans.id, scans.title, scans.tags,
                   COALESCE((SELECT GROUP_CONCAT(pages.ocrText, ' ') FROM pages WHERE pages.scanId = scans.id), '')
            FROM scans
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR REPLACE INTO page_ocr_artifacts(
                pageId, rawText, normalizedText, structuredContent, script, engine,
                engineVersion, pipelineVersion, sourceFingerprint, createdAt
            )
            SELECT id, ocrText, ocrText, '', 'LATIN', 'legacy', '0.2.9', 'legacy', sourceUri, 0
            FROM pages WHERE ocrText IS NOT NULL AND TRIM(ocrText) != ''
            """.trimIndent(),
        )
    }
}
