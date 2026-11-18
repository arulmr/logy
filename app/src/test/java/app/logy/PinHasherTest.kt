package app.logy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun verifiesOnlyTheMatchingPin() {
        val salt = ByteArray(16) { it.toByte() }
        val hash = PinHasher.hash("4826", salt)

        assertTrue(PinHasher.verify("4826", salt, hash))
        assertFalse(PinHasher.verify("4827", salt, hash))
    }

    @Test
    fun differentSaltsProduceDifferentHashes() {
        val first = PinHasher.hash("4826", ByteArray(16) { 1 })
        val second = PinHasher.hash("4826", ByteArray(16) { 2 })

        assertFalse(first.contentEquals(second))
    }
}
