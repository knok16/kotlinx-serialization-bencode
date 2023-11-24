package com.github.knok16.bencode

import java.nio.charset.Charset

class ParsingException(reason: String, val at: Int? = null) : Exception(reason)

sealed interface BencodedData

// TODO add documentation
class BencodedString(private val bytes: ByteArray) : BencodedData {
    val size: Int
        get() = bytes.size

    operator fun get(index: Int): Byte = bytes[index]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BencodedString

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    fun toString(charset: Charset): String = String(bytes, charset)

    override fun toString(): String = toString(Charsets.UTF_8)
}

@JvmInline
value class BencodedNumber(val value: Long) : BencodedData

@JvmInline
value class BencodedList(private val content: List<BencodedData>) : BencodedData, List<BencodedData> by content

@JvmInline
value class BencodedDictionary(private val content: Map<BencodedString, BencodedData>) : BencodedData,
    Map<BencodedString, BencodedData> by content

fun parse(byteArray: ByteArray): BencodedData? = parseRoot(Reader(byteArray))
