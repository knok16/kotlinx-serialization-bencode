package com.github.knok16.bencode

import com.github.knok16.serialization.Bencode
import kotlinx.serialization.decodeFromByteArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BencodeParserTest {
    private val charset = Charsets.US_ASCII

    private fun bencodedString(str: String) = BencodeString(str.toByteArray(charset))

    private fun bencodedListOf(vararg element: BencodeElement) = BencodeList(element.toList())

    private fun bencodedDictionaryOf(vararg element: Pair<String, BencodeElement>) =
        BencodeDictionary(element.associate { (key, value) -> bencodedString(key) to value })

    private inline fun <reified T> parse(string: String): T =
        Bencode.decodeFromByteArray(string.toByteArray(charset = charset))

    @Test
    fun decodeStringIsNotFullyParsable() {
        assertParsingException("Unexpected character 'U'", "UNEXPECTED_SYMBOLS")
        assertParsingException("Unexpected character 'i'", "i123ei456e")
    }

    @Test
    fun decodeEmptyString() {
        assertNull(parse<BencodeElement>(""))
        // TODO decide on this
        // assertNull(parse<BencodedString>(""))
        // assertNull(parse<BencodedNumber>(""))
        // assertNull(parse<BencodedList>(""))
        // assertNull(parse<BencodedDictionary>(""))
    }

    @Test
    fun decodeBencodedString() {
        assertEquals(bencodedString("abc"), parse<BencodeString>("3:abc"))
        assertEquals(bencodedString("AbcAbcAbcAbc"), parse<BencodeString>("12:AbcAbcAbcAbc"))
        assertEquals(bencodedString(""), parse<BencodeString>("0:"))
    }

    @Test
    fun decodeBencodedStringPrematureEndOfInput() {
        assertParsingException("Expected ':', but got end of input", "12")
        assertParsingException("Expected 12 more bytes in input to read a string", "12:")
        assertParsingException("Expected 9 more bytes in input to read a string", "12:abc")
        assertParsingException("Length of string too big: 123456789012", "123456789012:abc")
    }

    @Test
    fun decodeBencodedNumberSimple() {
        assertEquals(BencodeNumber(0), parse<BencodeNumber>("i0e"))
        assertEquals(BencodeNumber(1), parse<BencodeNumber>("i1e"))
        assertEquals(BencodeNumber(123), parse<BencodeNumber>("i123e"))
        assertEquals(BencodeNumber(72833), parse<BencodeNumber>("i72833e"))
    }

    @Test
    fun decodeBencodedNumberEmpty() {
        assertParsingException("Expected decimal digit, but got 'e'", "ie")
    }

    @Test
    fun decodeBencodedNumberNegativeValues() {
        assertEquals(BencodeNumber(-1), parse<BencodeNumber>("i-1e"))
        assertEquals(BencodeNumber(-123), parse<BencodeNumber>("i-123e"))
        assertEquals(BencodeNumber(-72833), parse<BencodeNumber>("i-72833e"))
    }

    @Test
    fun decodeBencodedNumberMaximumValues() {
        assertEquals(BencodeNumber(Long.MIN_VALUE), parse<BencodeNumber>("i${Long.MIN_VALUE}e"))
        assertEquals(BencodeNumber(Long.MAX_VALUE), parse<BencodeNumber>("i${Long.MAX_VALUE}e"))
    }

    @Test
    fun decodeBencodedNumberNonDigits() {
        assertParsingException("Expected 'e', but got 'N'", "i123NOT_DIGITSe")
    }

    @Test
    fun decodeBencodedNumberPrematureEndOfInput() {
        assertParsingException("Expected 'e', but got end of input", "i123")
    }

    @Test
    fun decodeBencodedNumberUnaryPlus() {
        assertParsingException("Expected decimal digit, but got '+'", "i+123e")
    }

    @Test
    fun decodeBencodedList() {
        assertEquals(bencodedListOf(bencodedString("hello"), BencodeNumber(52)), parse<BencodeList>("l5:helloi52ee"))
    }

    @Test
    fun decodeBencodedEmptyList() {
        assertEquals(bencodedListOf(), parse<BencodeList>("le"))
    }

    @Test
    fun decodeBencodedListPrematureEnd() {
        assertParsingException("Expected 'e', but got end of input", "l")
        assertParsingException("Expected 'e', but got end of input", "li123e")
        assertParsingException("Expected 'e', but got end of input", "l3:abc")
    }

    @Test
    fun decodeBencodedDictionary() {
        assertEquals(
            bencodedDictionaryOf("foo" to bencodedString("bar"), "hello" to BencodeNumber(52)),
            parse<BencodeDictionary>("d3:foo3:bar5:helloi52ee")
        )
    }

    @Test
    fun decodeBencodedEmptyDictionary() {
        assertEquals(bencodedDictionaryOf(), parse<BencodeDictionary>("de"))
    }

    @Test
    fun decodeBencodedDictionaryPrematureEnd() {
        assertParsingException("Expected 'e', but got end of input", "d")
        assertParsingException("Cannot parse dictionary value for key 'abc'", "d3:abc")
        assertParsingException("Expected 'e', but got end of input", "d3:abc3:bar")
    }

    @Test
    fun decodeBencodedDictionaryOnlyStringAllowedAsKeys() {
        assertParsingException("Only strings allowed as keys in dictionary", "di123e5:valuee")
        assertParsingException("Only strings allowed as keys in dictionary", "dl5:helloi52ee5:valuee")
        assertParsingException("Only strings allowed as keys in dictionary", "dde3:abce")
    }

    @Test
    fun decodeBencodedNestedObjects() {
        assertEquals(
            bencodedDictionaryOf(
                "foo" to bencodedListOf(
                    bencodedString("hello"),
                    BencodeNumber(52),
                    bencodedDictionaryOf(),
                    bencodedListOf()
                ),
                "hello" to BencodeNumber(52)
            ),
            parse<BencodeDictionary>("d3:fool5:helloi52edelee5:helloi52ee")
        )
    }

    fun assertParsingException(expectedReason: String, bencodedString: String) = assertEquals(
        expectedReason,
        assertFailsWith<ParsingException> { parse<BencodeElement>(bencodedString) }.message
    )
}
