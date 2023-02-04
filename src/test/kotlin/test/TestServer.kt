package test

import kotlin.test.*

class TestServer : ServletTestSupport() {

    @Test
    fun testHTTPGenerate() {
        assertNotNull(getId())
    }

    @Test
    fun testHTTPGenerateMultiple() {
        assertEquals(10, getIds(10).items.size)
        assertEquals(100, getIds(100).items.size)
    }

    @Test
    fun testHTTPGenerateTooMAny() {
        assertContains(
            getId(422, n = "10000000"),
            "error",
        )
    }

    @Test
    fun testHTTPGenerateMultipleInvalidIdFormat() {
        getId(422, n = "test_amount_is_invalid_int")
    }

    @Test
    fun testHTTPGenerateMultipleNegative() {
        getId(422, n = "-200")
    }

    @Test
    fun testHTTPDocs() {
        assertNotEquals(0, get("/docs").length)
    }

    @Test
    fun testUnknownPath() {
        assertContains(
            get(
                "/unknown",
                contentType = "application/json",
                status = 404,
            ),
            "error",
        )
    }

    @Test
    fun testDropsTrailingSlash() {
        assertContains(
            get(
                "/docs/",
                status = 200,
            ),
            "IDGen",
        )
    }

    @Test
    fun testNullPathEqualsRoot() {
        assertContains(
            get(
                null,
                contentType = "application/json",
                status = 200,
            ),
            "id",
        )
    }


}