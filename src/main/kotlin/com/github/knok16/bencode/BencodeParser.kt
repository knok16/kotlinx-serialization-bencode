package com.github.knok16.bencode

private val DIGITS_RANGE = '0'..'9'
private const val END_TOKEN = 'e'
private const val NUMBER_START_TOKEN = 'i'
private const val LIST_START_TOKEN = 'l'
private const val DICTIONARY_START_TOKEN = 'd'

internal fun parseRoot(reader: Reader): BencodedData? {
    val result = parse(reader)

    if (reader.peek() != null)
        throw ParsingException("Unexpected character '${reader.peek()}'", reader.index)

    return result
}

internal fun parse(reader: Reader): BencodedData? = when (reader.peek()) {
    in DIGITS_RANGE -> parseString(reader)
    NUMBER_START_TOKEN -> parseNumber(reader)
    LIST_START_TOKEN -> parseList(reader)
    DICTIONARY_START_TOKEN -> parseDictionary(reader)
    else -> null
}

internal fun parseInteger(reader: Reader): Long {
    when (val token = reader.peek()) {
        null -> throw ParsingException("Expected decimal digit, but got end of input")
        !in DIGITS_RANGE -> throw ParsingException("Expected decimal digit, but got '$token'")
    }

    var result = 0L
    while (reader.peek() in DIGITS_RANGE) {
        result = result * 10 + (reader.next()!! - '0')
    }

    return result
}

internal fun parseString(reader: Reader): BencodedString {
    val len = parseInteger(reader)
    if (len > Integer.MAX_VALUE)
        throw ParsingException("Length of string too big: $len")

    reader.assertNextToken(':')

    return BencodedString(reader.takeNext(len.toInt()))
}

internal fun parseNumber(reader: Reader): BencodedNumber {
    reader.assertNextToken(NUMBER_START_TOKEN)

    val minusSign = reader.peek() == '-'
    if (minusSign)
        reader.next() // pop minus sign

    val result = parseInteger(reader)

    reader.assertNextToken(END_TOKEN)

    return BencodedNumber(if (minusSign) -result else result)
}

internal fun parseList(reader: Reader): BencodedList {
    reader.assertNextToken(LIST_START_TOKEN)

    val result = ArrayList<BencodedData>()

    while (true) {
        result.add(parse(reader) ?: break)
    }

    reader.assertNextToken(END_TOKEN)

    return BencodedList(result)
}

internal fun parseDictionary(reader: Reader): BencodedDictionary {
    reader.assertNextToken(DICTIONARY_START_TOKEN)

    val result = HashMap<BencodedString, BencodedData>()

    while (true) {
        val keyStartingIndex = reader.index
        val key = parse(reader) ?: break
        if (key !is BencodedString)
            throw ParsingException("Only strings allowed as keys in dictionary", keyStartingIndex)

        val valueStartingIndex = reader.index
        val value = parse(reader)
            ?: throw ParsingException("Cannot parse dictionary value for key '$key'", valueStartingIndex)

        result[key] = value
    }

    reader.assertNextToken(END_TOKEN)

    return BencodedDictionary(result)
}

private fun Reader.assertNextToken(expectedToken: Char) = when (val token = next()) {
    null -> throw ParsingException("Expected '$expectedToken', but got end of input")
    expectedToken -> {}
    else -> throw ParsingException("Expected '$expectedToken', but got '$token'", prevIndex)
}
