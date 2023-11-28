package com.github.knok16.bencode

import java.nio.charset.Charset

class Reader(private val input: ByteArray)  {
    var index = 0
        private set

    fun peek() = if (index < input.size) input[index].toInt().toChar() else null

    // TODO should it return Byte?
    fun next() = peek()?.also { index++ }

    fun takeNextBytes(n: Int): ByteArray = if (index + n <= input.size)
        input.copyOfRange(index, index + n).also { index += n }
    else
        throw ParsingException("Expected ${index + n - input.size} more bytes in input to read a string")

    // TODO deduplicate validation?
    fun takeString(length: Int, charset: Charset): String = if (index + length <= input.size)
        String(input, index, length, charset).also { index += length }
    else
        throw ParsingException("Expected ${index + length - input.size} more bytes in input to read a string")
}
