package com.github.knok16.bencode

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.util.*

private val byteArraySerializer = serializer<ByteArray>()
private val bencodedStringSerializer = serializer<BencodeString>()
private val bencodeElementSerializer = serializer<BencodeElement>()

// TODO add support for sequential decoding?
@OptIn(ExperimentalSerializationApi::class)
class BencodeDecoder(
    private val bencode: Bencode,
    private val reader: Reader
) : AbstractDecoder() {
    override val serializersModule: SerializersModule
        get() = bencode.serializersModule

    private val positions = Stack<Int>()

    override fun decodeString(): String =
        reader.readString(charset = bencode.stringCharset)

    // TODO add validation if value too big for byte/short/int
    // TODO what about supporting BigInteger?
    // TODO should it even be supported or decoder should support Long only, and any conversions should be done on top of it by serializer
    override fun decodeByte(): Byte =
        decodeLong().toByte()

    override fun decodeShort(): Short =
        decodeLong().toShort()

    override fun decodeInt(): Int =
        decodeLong().toInt()

    override fun decodeChar(): Char =
        decodeInt().toChar()

    override fun decodeLong(): Long =
        reader.readNumber()

    fun decodeByteArray(): ByteArray =
        reader.readByteString()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        when (descriptor.kind) {
            StructureKind.LIST -> reader.consumeToken(LIST_START_TOKEN)

            StructureKind.MAP,
            StructureKind.CLASS,
            StructureKind.OBJECT -> reader.consumeToken(DICTIONARY_START_TOKEN)

            else -> TODO("Add proper error message")
        }
        positions.push(0)
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        reader.consumeToken(END_TOKEN)
        positions.pop()
    }

    override tailrec fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (reader.peek() == END_TOKEN) {
            CompositeDecoder.DECODE_DONE
        } else when (descriptor.kind) {
            StructureKind.LIST,
            StructureKind.MAP -> positions.pop().also { positions.push(it + 1) }

            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                val index = descriptor.getElementIndex(decodeString())
                if (index == CompositeDecoder.UNKNOWN_NAME && bencode.ignoreUnknownKeys) {
                    reader.readBencodeElement() // read and throw-out value for unknown key
                    decodeElementIndex(descriptor)
                } else index
            }

            else -> TODO("Add proper error message")
        }
    }

    // TODO which one should be overridden? this one or with 2 parameters?
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        when (deserializer.descriptor) {
            // TODO should it be added on top, by some standalone serializer?
            byteArraySerializer.descriptor -> when (reader.peek()) {
                in DIGITS_RANGE -> decodeByteArray() as T
                else -> super.decodeSerializableValue(deserializer)
            }
            // TODO next line will be obsolete if find a way to make BencodedString as value class
            bencodedStringSerializer.descriptor -> BencodeString(reader.readByteString()) as T
            bencodeElementSerializer.descriptor -> reader.readBencodeElement() as T
            else -> super.decodeSerializableValue(deserializer)
        }
}
