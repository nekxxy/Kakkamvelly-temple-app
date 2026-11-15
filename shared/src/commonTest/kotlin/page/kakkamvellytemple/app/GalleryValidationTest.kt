package page.kakkamvellytemple.app

import kotlin.test.*
import page.kakkamvellytemple.app.data.model.GalleryValidation
import page.kakkamvellytemple.app.data.remote.GalleryDto

/** Gallery entry validation. Photo files live on the website host and the app
 *  carries only the slug; the visitor screen silently coerces a bad slug, which
 *  would render the WRONG photo with no error - so the admin editor must reject
 *  bad entries before they can be published. */
class GalleryValidationTest {

    private fun photo(slug: String, ml: String = "അ", en: String = "a") = GalleryDto(slug, ml, en)

    @Test fun validSlugPasses() {
        assertNull(GalleryValidation.slugError("temple-front-2026"))
    }

    @Test fun blankSlugFails() {
        assertNotNull(GalleryValidation.slugError(""))
        assertNotNull(GalleryValidation.slugError("   "))
    }

    @Test fun uppercaseSpaceAndPathCharsFail() {
        assertNotNull(GalleryValidation.slugError("Temple"))
        assertNotNull(GalleryValidation.slugError("my photo"))
        assertNotNull(GalleryValidation.slugError("../etc"))
        assertNotNull(GalleryValidation.slugError("a.jpg"), "dots are not slug chars")
    }

    @Test fun overlongSlugFails() {
        assertNotNull(GalleryValidation.slugError("a".repeat(GalleryValidation.MAX_SLUG_LEN + 1)))
        assertNull(GalleryValidation.slugError("a".repeat(GalleryValidation.MAX_SLUG_LEN)))
    }

    @Test fun overlongCaptionFails() {
        val errors = GalleryValidation.rowErrors(listOf(photo("ok", en = "x".repeat(GalleryValidation.MAX_CAPTION_LEN + 1))))
        assertEquals(setOf(0), errors.keys)
    }

    @Test fun duplicateSlugsFlagSecondRow() {
        val errors = GalleryValidation.rowErrors(listOf(photo("a"), photo("b"), photo("a")))
        assertEquals(setOf(2), errors.keys)
        assertTrue(errors[2]!!.contains("Duplicate"))
    }

    @Test fun validGalleryHasNoRowErrors() {
        assertTrue(GalleryValidation.rowErrors(listOf(photo("a"), photo("b-2"))).isEmpty())
    }

    @Test fun galleryCap() {
        val full = (1..GalleryValidation.MAX_PHOTOS).map { photo("p$it") }
        assertNotNull(GalleryValidation.fullError(full))
        assertNull(GalleryValidation.fullError(full.dropLast(1)))
    }
}
