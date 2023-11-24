package com.github.knok16.bencode

internal class Reader(private val input: ByteArray)  {
    var index = 0
        private set

    fun peek() = if (index < input.size) input[index].toInt().toChar() else null

    // TODO should it return Byte?
    fun next() = peek()?.also { index++ }

    fun takeNext(n: Int): ByteArray = if (index + n <= input.size)
        input.copyOfRange(index, index + n).also { index += n }
    else
        throw ParsingException("Expected ${index + n - input.size} more bytes in input to read a string")

    val prevIndex: Int
        get() = index - 1
}
