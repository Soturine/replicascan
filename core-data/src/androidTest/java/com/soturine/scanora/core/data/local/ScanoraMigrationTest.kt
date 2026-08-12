package com.soturine.scanora.core.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanoraMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ScanoraDatabase::class.java,
    )

    @Test
    fun exportedVersionOneSchemaCanBeCreatedAndValidated() {
        helper.createDatabase(TEST_DATABASE, 1).close()
        helper.runMigrationsAndValidate(TEST_DATABASE, 1, true).close()
    }

    companion object {
        private const val TEST_DATABASE = "scanora-migration-test"
    }
}
