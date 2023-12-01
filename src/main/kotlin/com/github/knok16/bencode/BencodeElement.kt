package com.github.knok16.bencode

import kotlinx.serialization.Serializable
import java.nio.charset.Charset

/**
 * Interface representing single Bencode element.
 * Can be [BencodeString], [BencodeNumber], [BencodeList] or [BencodeDictionary].
 */
sealed interface BencodeElement

/**
 * Class representing Bencode Byte String.
 */
@Serializable
class BencodeString(private val bytes: ByteArray) : BencodeElement {
    val size: Int
        get() = bytes.size

    operator fun get(index: Int): Byte = bytes[index]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BencodeString

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    fun toString(charset: Charset): String = String(bytes, charset)

    override fun toString(): String = toString(Charsets.UTF_8)
}

/**
 * Class representing Bencode Integer.
 */
@JvmInline
@Serializable
value class BencodeNumber(val value: Long) : BencodeElement

/**
 * Class representing Bencode List, consisting of indexed values, where value is arbitrary [BencodeElement]
 *
 * Since this class also implements [List] interface, you can use
 * traditional methods like [List.get] or [List.getOrNull] to obtain elements.
 */
@JvmInline
@Serializable
value class BencodeList(private val content: List<BencodeElement>) : BencodeElement, List<BencodeElement> by content

/**
 * Class representing Bencode Dictionary, consisting of name-value pairs,
 * where key is [BencodeString] and value is arbitrary [BencodeElement]
 *
 * Since this class also implements [Map] interface, you can use
 * traditional methods like [Map.get] or [Map.getValue] to obtain elements.
 */
@JvmInline
@Serializable
value class BencodeDictionary(private val content: Map<BencodeString, BencodeElement>) : BencodeElement,
    Map<BencodeString, BencodeElement> by content
