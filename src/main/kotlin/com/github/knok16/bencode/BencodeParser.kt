package com.github.knok16.bencode

import java.nio.charset.Charset

object DIGITS_RANGE {
    operator fun contains(token: Char?) = token in '0'..'9'
}

const val END_TOKEN = 'e'
const val NUMBER_START_TOKEN = 'i'
const val LIST_START_TOKEN = 'l'
const val DICTIONARY_START_TOKEN = 'd'

fun Reader.readData(): BencodeElement? = when (peek()) {
    in DIGITS_RANGE -> BencodeString(readByteString())
    NUMBER_START_TOKEN -> BencodeNumber(readNumber())
    LIST_START_TOKEN -> BencodeList(readList())
    DICTIONARY_START_TOKEN -> BencodeDictionary(readDictionary())
    else -> null
}

internal fun Reader.readInteger(): Long {
    when (val token = peek()) {
        null -> throw ParsingException("Expected decimal digit, but got end of input")
        !in DIGITS_RANGE -> throw ParsingException("Expected decimal digit, but got '$token'", index)
    }

    var result = 0L
    while (peek() in DIGITS_RANGE) {
        result = result * 10 + (next()!! - '0')
    }

    return result
}

fun Reader.readByteString(): ByteArray {
    val len = readInteger()
    if (len > Integer.MAX_VALUE)
        throw ParsingException("Length of string too big: $len")

    consumeToken(':')

    return takeNextBytes(len.toInt())
}

// TODO deduplicate common with readByteString
fun Reader.readString(charset: Charset): String {

    val len = readInteger()
    if (len > Integer.MAX_VALUE)
        throw ParsingException("Length of string too big: $len")

    consumeToken(':')

    return takeString(len.toInt(), charset)
}

fun Reader.readNumber(): Long {
    consumeToken(NUMBER_START_TOKEN)

    val minusSign = peek() == '-'
    if (minusSign)
        next() // pop minus sign

    val result = readInteger()

    consumeToken(END_TOKEN)

    return if (minusSign) -result else result
}

fun Reader.readList(): List<BencodeElement> {
    consumeToken(LIST_START_TOKEN)

    val result = generateSequence { readData() }.toList()

    consumeToken(END_TOKEN)

    return result
}

fun Reader.readDictionary(): Map<BencodeString, BencodeElement> {
    consumeToken(DICTIONARY_START_TOKEN)

    val result = generateSequence {
        val keyStartingIndex = index
        readData()?.let { key ->
            if (key !is BencodeString)
                throw ParsingException("Only strings allowed as keys in dictionary", keyStartingIndex)

            val valueStartingIndex = index
            val value = readData()
                ?: throw ParsingException("Cannot parse dictionary value for key '$key'", valueStartingIndex)

            key to value
        }
    }.toMap()

    consumeToken(END_TOKEN)

    return result
}

fun Reader.consumeToken(expectedToken: Char) = when (val token = peek()) {
    null -> throw ParsingException("Expected '$expectedToken', but got end of input")
    expectedToken -> next()
    else -> throw ParsingException("Expected '$expectedToken', but got '$token'", index)
}
