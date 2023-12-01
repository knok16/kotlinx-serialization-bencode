package com.github.knok16.bencode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlin.test.*

class BencodeParserTest {
    private val charset = Charsets.UTF_8

    private fun bencodedString(str: String) = BencodeString(str.toByteArray(charset))

    private fun bencodedListOf(vararg element: BencodeElement) = BencodeList(element.toList())

    private fun bencodedDictionaryOf(vararg element: Pair<String, BencodeElement>) =
        BencodeDictionary(element.associate { (key, value) -> bencodedString(key) to value })

    private inline fun <reified T> parse(string: String): T =
        Bencode.decodeFromByteArray(string.toByteArray(charset = charset))

    private fun readBytesFromResource(resourceName: String): ByteArray =
        BencodeParserTest::class.java.getResourceAsStream(resourceName)?.readBytes()
            ?: throw IllegalArgumentException("Cannot find resource '$resourceName'")

    private fun assertParsingException(expectedReason: String, bencodedString: String) = assertEquals(
        expectedReason,
        assertFailsWith<ParsingException> { parse<BencodeElement>(bencodedString) }.message
    )

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

    @Serializable
    data class TorrentMetadata(
        val announce: String,
        val publisher: String? = null,
        @SerialName("creation date")
        val creationDate: Long,
        @SerialName("created by")
        val createdBy: String,
        val encoding: String? = null,
        val comment: String?,
        @SerialName("announce-list")
        val announceList: List<List<String>>,
        @SerialName("publisher-url")
        val publisherUrl: String? = null,
        val info: Info
    )

    @Serializable
    data class Info(
        val private: Long = 0,
        val length: Long,
        val pieces: ByteArray,
        @SerialName("piece length")
        val pieceLength: Long,
        val name: String
    )

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun decodeFromByteArray() {
        val bytes = readBytesFromResource("/ubuntu-23.10.1-desktop-amd64.iso.torrent")

        val metadata = Bencode.decodeFromByteArray<TorrentMetadata>(bytes)

        assertEquals("https://torrent.ubuntu.com/announce", metadata.announce)
        assertEquals(null, metadata.publisher)
        assertEquals(1697466120, metadata.creationDate)
        assertEquals("mktorrent 1.1", metadata.createdBy)
        assertEquals(null, metadata.encoding)
        assertEquals("Ubuntu CD releases.ubuntu.com", metadata.comment)
        assertEquals(
            listOf(
                listOf("https://torrent.ubuntu.com/announce"),
                listOf("https://ipv6.torrent.ubuntu.com/announce")
            ), metadata.announceList
        )
        assertEquals(null, metadata.publisherUrl)
        assertEquals(0, metadata.info.private)
        assertEquals(5173995520, metadata.info.length)
        assertEquals(394760, metadata.info.pieces.size)
        assertContentEquals(
            ubyteArrayOf(0xB5u, 0x93u, 0x68u, 0x26u, 0x38u, 0xEBu, 0xD2u, 0x40u, 0x32u, 0x73u).toByteArray(),
            metadata.info.pieces.copyOf(10)
        )
        assertContentEquals(
            ubyteArrayOf(0xD1u, 0x92u, 0x68u, 0xDDu, 0x23u, 0x11u, 0xE2u, 0x8Du, 0x1Cu, 0x3Du).toByteArray(),
            metadata.info.pieces.copyOfRange(394760 - 10, 394760)
        )
        assertEquals(262144, metadata.info.pieceLength)
        assertEquals("ubuntu-23.10.1-desktop-amd64.iso", metadata.info.name)
    }

    @Test
    fun decodeString() {
        assertEquals(
            "AbcdAbcdAbcdA",
            parse("13:AbcdAbcdAbcdA")
        )
    }

    @Test
    fun decodeStringMultiByteChars() {
        assertEquals(
            "§§§§",
            parse("8:§§§§")
        )
    }

    @Test
    fun decodeByteArrayFromByteString() {
        assertContentEquals(
            "AbcdAbcdAbcdA".toByteArray(charset = charset),
            parse("13:AbcdAbcdAbcdA")
        )
    }

    @Test
    fun decodeByte() {
        assertEquals(
            123.toByte(),
            parse("i123e")
        )
    }

    @Test
    fun decodeShort() {
        assertEquals(
            123.toShort(),
            parse("i123e")
        )
    }

    @Test
    fun decodeChar() {
        assertEquals(
            '@',
            parse("i64e")
        )
    }

    @Test
    fun decodeInt() {
        assertEquals(
            123,
            parse("i123e")
        )
    }

    @Test
    fun decodeLong() {
        assertEquals(
            123L,
            parse("i123e")
        )
    }

    @Test
    fun decodeEmptyList() {
        assertEquals(
            emptyList<String>(),
            parse("le")
        )
    }

    @Test
    fun decodeList() {
        assertEquals(
            listOf("abc", "foo", "bar"),
            parse("l3:abc3:foo3:bare")
        )
    }

    @Test
    fun decodeEmptyArray() {
        assertContentEquals(
            emptyArray<String>(),
            parse("le")
        )
    }

    @Test
    fun decodeArray() {
        assertContentEquals(
            arrayOf("abc", "foo", "bar"),
            parse("l3:abc3:foo3:bare")
        )
    }

    @Test
    @Ignore // TODO fix decoder
    fun decodeByteArrayFromList() {
        assertContentEquals(
            byteArrayOf(78, 79, 73, 67, 69),
            parse("li78ei79ei73ei67ei69ee")
        )
    }

    @Test
    fun decodeShortArray() {
        assertContentEquals(
            shortArrayOf(78, 79, 73, 67, 69),
            parse("li78ei79ei73ei67ei69ee")
        )
    }

    @Test
    fun decodeCharArray() {
        assertContentEquals(
            charArrayOf('N', 'O', 'I', 'C', 'E'),
            parse("li78ei79ei73ei67ei69ee")
        )
    }

    @Test
    fun decodeIntArray() {
        assertContentEquals(
            intArrayOf(78, 79, 73, 67, 69),
            parse("li78ei79ei73ei67ei69ee")
        )
    }

    @Test
    fun decodeLongArray() {
        assertContentEquals(
            longArrayOf(78, 79, 73, 67, 69),
            parse("li78ei79ei73ei67ei69ee")
        )
    }

    @Test
    fun decodeMapStringKey() {
        assertEquals(
            mapOf("abc" to "def", "foo" to "bar"),
            parse("d3:abc3:def3:foo3:bare")
        )
    }

    @Test
    fun decodeMapBencodeStringKey() {
        assertEquals(
            mapOf(
                BencodeString("abc".toByteArray(charset)) to "def",
                BencodeString("foo".toByteArray(charset)) to "bar"
            ),
            parse("d3:abc3:def3:foo3:bare")
        )
    }

    @Test
    fun decodeMapByteArrayKey() {
        val actual = parse<Map<ByteArray, String>>("d3:abc3:def3:foo3:bare").toList().sortedBy { it.second }

        assertEquals(2, actual.size)

        assertContentEquals("foo".toByteArray(charset), actual[0].first)
        assertEquals("bar", actual[0].second)

        assertContentEquals("abc".toByteArray(charset), actual[1].first)
        assertEquals("def", actual[1].second)
    }

    @Serializable
    data class POJO(
        val abc: String,
        val foo: String
    )

    @Test
    fun decodePOJO() {
        assertEquals(
            POJO(
                abc = "def",
                foo = "bar"
            ),
            parse("d3:abc3:def3:foo3:bare")
        )
    }

    @Serializable
    data class R(
        val name: String,
        val inner: R? = null
    )

    @Serializable
    data class A(
        val stringField: String,
        val list: List<Map<String, R>>,
        val byteField: Byte
    )

    @Test
    fun decodeNested() {
        assertEquals(
            A(
                stringField = "foo",
                list = listOf(
                    emptyMap(),
                    mapOf("bar" to R(name = "alice", inner = R(name = "bob"))),
                    emptyMap()
                ),
                byteField = 12
            ),
            parse("d9:byteFieldi12e11:stringField3:foo4:listlded3:bard4:name5:alice5:innerd4:name3:bobeeedeee")
        )
    }

    @Serializable
    data class IgnoreUnknownKeysExample(
        val knownProperty: String
    )

    @Test
    fun ignoreUnknownKeysSmallExample() {
        val bytes = "d13:knownProperty3:foo15:unknownProperty3:bare".toByteArray()

        val result = Bencode {
            ignoreUnknownKeys = true
        }.decodeFromByteArray<IgnoreUnknownKeysExample>(bytes)

        assertEquals(IgnoreUnknownKeysExample(knownProperty = "foo"), result)
    }

    @Serializable
    data class TorrentMetadataSmall(
        val announce: String,
        val info: Info
    )

    @Test
    fun ignoreUnknownKeys() {
        val bytes = readBytesFromResource("/ubuntu-23.10.1-desktop-amd64.iso.torrent")

        val metadata = Bencode {
            ignoreUnknownKeys = true
        }.decodeFromByteArray<TorrentMetadataSmall>(bytes)

        assertEquals("https://torrent.ubuntu.com/announce", metadata.announce)
    }

    @Test
    fun stringCharsetConfiguration() {
        val bytes = byteArrayOf('6'.code.toByte(), ':'.code.toByte()) + "abc".toByteArray(Charsets.UTF_16LE)

        assertEquals(8, bytes.size)
        assertEquals("abc", Bencode {
            stringCharset = Charsets.UTF_16LE
        }.decodeFromByteArray<String>(bytes))
    }
}
