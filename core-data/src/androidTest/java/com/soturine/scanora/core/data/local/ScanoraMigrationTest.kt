package com.soturine.scanora.core.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.common.truth.Truth.assertThat

@RunWith(AndroidJUnit4::class)
class ScanoraMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ScanoraDatabase::class.java,
    )

    @Test
    fun migratesVersionOneWithoutLosingScansPagesOrLegacyOcr() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO scans(id,title,mode,tags,isFavorite,createdAt,updatedAt,isDraft) " +
                    "VALUES('scan-1','Receipt','receipt','tax|2026',0,1,1,0)",
            )
            execSQL(
                "INSERT INTO pages(id,scanId,pageIndex,sourceUri,processedUri,filterType,rotationDegrees,quad,ocrText) " +
                    "VALUES('page-1','scan-1',0,'content://source',NULL,'original_corrected',0,NULL,'Total 42')",
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, MIGRATION_1_2).use { database ->
            assertThat(database.scalarLong("SELECT COUNT(*) FROM scans")).isEqualTo(1)
            assertThat(database.scalarLong("SELECT COUNT(*) FROM pages")).isEqualTo(1)
            assertThat(database.scalarLong("SELECT COUNT(*) FROM page_ocr_artifacts")).isEqualTo(1)
            assertThat(database.scalarLong("SELECT COUNT(*) FROM scan_search_fts WHERE scan_search_fts MATCH 'Total'")).isEqualTo(1)
        }
    }

    private fun SupportSQLiteDatabase.scalarLong(query: String): Long =
        this.query(query).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }

    companion object {
        private const val TEST_DATABASE = "scanora-migration-test"
    }
}
