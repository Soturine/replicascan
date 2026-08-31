package com.soturine.replicascan.core.data.files

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Before
import org.junit.Test

class ManagedFilePolicyTest {
    private lateinit var root: File
    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var policy: ManagedFilePolicy

    @Before
    fun setUp() {
        root = Files.createTempDirectory("replicascan-policy").toFile()
        filesDir = File(root, "files").apply { mkdirs() }
        cacheDir = File(root, "cache").apply { mkdirs() }
        policy = ManagedFilePolicy(filesDir, cacheDir)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun resolvesOnlyKnownManagedNamespaces() {
        val source = File(policy.sourceDirectory, "source.jpg").apply { parentFile?.mkdirs() }
        val derived = File(policy.derivedDirectory, "preview.jpg").apply { parentFile?.mkdirs() }
        val shared = File(policy.sharedExportDirectory, "share.pdf").apply { parentFile?.mkdirs() }

        assertThat(policy.resolve(source.absolutePath)?.kind).isEqualTo(ManagedFileKind.SOURCE)
        assertThat(policy.resolve(derived.toURI().toString())?.kind).isEqualTo(ManagedFileKind.DERIVED)
        assertThat(policy.resolve(shared.absolutePath)?.kind).isEqualTo(ManagedFileKind.SHARED_EXPORT)
        assertThat(policy.resolve(File(root, "external.jpg").absolutePath)).isNull()
        assertThat(policy.resolve("content://photos/external/1")).isNull()
    }

    @Test
    fun rejectsTraversalOutsideManagedDirectory() {
        val traversal = File(policy.sourceDirectory, "../outside.jpg").path

        assertThat(policy.resolve(traversal)).isNull()
    }
}
