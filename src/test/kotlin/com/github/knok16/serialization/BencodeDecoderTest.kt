package com.github.knok16.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class BencodeDecoderTest {
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

    private fun readBytesFromResource(resourceName: String): ByteArray =
        BencodeDecoderTest::class.java.getResourceAsStream(resourceName)?.readBytes()
            ?: throw IllegalArgumentException("Cannot find resource '$resourceName'")

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
            listOf(0xB5, 0x93, 0x68, 0x26, 0x38, 0xEB, 0xD2, 0x40, 0x32, 0x73).map { it.toByte() }.toByteArray(),
            metadata.info.pieces.copyOf(10)
        )
        assertContentEquals(
            listOf(0xD1, 0x92, 0x68, 0xDD, 0x23, 0x11, 0xE2, 0x8D, 0x1C, 0x3D).map { it.toByte() }.toByteArray(),
            metadata.info.pieces.copyOfRange(394760 - 10, 394760)
        )
        assertEquals(262144, metadata.info.pieceLength)
        assertEquals("ubuntu-23.10.1-desktop-amd64.iso", metadata.info.name)
    }
}