package com.github.knok16.bencode

import java.nio.charset.Charset

object DIGITS_RANGE {
    const val LOWER_BOUND = '0'.code.toByte()
    const val UPPER_BOUND = '9'.code.toByte()

    operator fun contains(token: Token?) = token != null && LOWER_BOUND <= token && token <= UPPER_BOUND
}

const val END_TOKEN = 'e'.code.toByte()
const val BYTE_STRING_LENGTH_AND_DATA_SEPARATOR = ':'.code.toByte()
const val NUMBER_START_TOKEN = 'i'.code.toByte()
const val NUMBER_MINUS_SIGN_TOKEN = '-'.code.toByte()
const val LIST_START_TOKEN = 'l'.code.toByte()
const val DICTIONARY_START_TOKEN = 'd'.code.toByte()

// TODO should we show it as byte?
internal fun Token.tokenToChar(): String = "'${toInt().toChar()}'"

fun Reader.unexpectedToken(expected: String): Nothing = throw peek()?.let {
    ParsingException("Expected $expected, but got ${it.tokenToChar()}", index)
} ?: ParsingException("Expected $expected, but got end of input")

fun Reader.readBencodeElement(): BencodeElement = when (peek()) {
    in DIGITS_RANGE -> BencodeString(readByteString())
    NUMBER_START_TOKEN -> BencodeNumber(readNumber())
    LIST_START_TOKEN -> BencodeList(readList())
    DICTIONARY_START_TOKEN -> BencodeDictionary(readDictionary())
    else -> unexpectedToken("start of bencode element")
}

internal fun <T> Reader.readUntilEndToken(readAction: Reader.() -> T): Sequence<T> = generateSequence {
    if (peek() != END_TOKEN) readAction() else null
}

internal fun Reader.readInteger(): Long {
    if (peek() !in DIGITS_RANGE)
        unexpectedToken("decimal digit")

    var result = 0L
    while (peek() in DIGITS_RANGE) {
        result = result * 10 + (next() - DIGITS_RANGE.LOWER_BOUND)
    }

    return result
}

fun Reader.readByteString(): ByteArray {
    val len = readInteger()
    if (len > Integer.MAX_VALUE)
        throw ParsingException("Length of string too big: $len")

    consumeToken(BYTE_STRING_LENGTH_AND_DATA_SEPARATOR)

    return takeNextBytes(len.toInt())
}

// TODO deduplicate common with readByteString
fun Reader.readString(charset: Charset): String {
    val len = readInteger()
    if (len > Integer.MAX_VALUE)
        throw ParsingException("Length of string too big: $len")

    consumeToken(BYTE_STRING_LENGTH_AND_DATA_SEPARATOR)

    return takeString(len.toInt(), charset)
}

fun Reader.readNumber(): Long {
    consumeToken(NUMBER_START_TOKEN)

    val minusSign = peek() == NUMBER_MINUS_SIGN_TOKEN
    if (minusSign)
        next() // pop minus sign

    val result = readInteger()

    consumeToken(END_TOKEN)

    return if (minusSign) -result else result
}

fun Reader.readList(): List<BencodeElement> {
    consumeToken(LIST_START_TOKEN)

    val result = readUntilEndToken { readBencodeElement() }.toList()

    consumeToken(END_TOKEN)

    return result
}

fun Reader.readDictionary(): Map<BencodeString, BencodeElement> {
    consumeToken(DICTIONARY_START_TOKEN)

    val result = readUntilEndToken { BencodeString(readByteString()) to readBencodeElement() }.toMap()

    consumeToken(END_TOKEN)

    return result
}

fun Reader.consumeToken(expectedToken: Token) =
    if (peek() == expectedToken) next()
    else unexpectedToken(expectedToken.tokenToChar())
