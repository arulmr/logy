package app.logy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {
    @Test fun detectsNewerSemanticVersion() {
        assertTrue(UpdateManager.isNewer("2.2.0", "2.1.0"))
        assertTrue(UpdateManager.isNewer("3.0", "2.9.9"))
    }

    @Test fun rejectsSameOrOlderVersion() {
        assertFalse(UpdateManager.isNewer("2.1", "2.1.0"))
        assertFalse(UpdateManager.isNewer("2.0.9", "2.1.0"))
    }
}
