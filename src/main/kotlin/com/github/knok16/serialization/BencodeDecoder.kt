@file:OptIn(ExperimentalSerializationApi::class)

package com.github.knok16.serialization

import com.github.knok16.bencode.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.util.*

private val byteArraySerializer = serializer<ByteArray>()

// TODO add support for sequential decoding?
@OptIn(ExperimentalSerializationApi::class)
class BencodeDecoder(
    private val reader: Reader,
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : AbstractDecoder() {
    private val positions = Stack<Int>()

    override fun decodeString(): String =
        reader.readString(charset = Charsets.UTF_8) // TODO move charset into parameters

    override fun decodeInt(): Int =
        reader.readNumber().toInt() // TODO add validation if too big

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

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (reader.peek() in setOf(null, END_TOKEN)) {
            CompositeDecoder.DECODE_DONE
        } else when (descriptor.kind) {
            StructureKind.LIST,
            StructureKind.MAP -> positions.pop().also { positions.push(it + 1) }

            StructureKind.CLASS,
            StructureKind.OBJECT -> descriptor.getElementIndex(decodeString())

            else -> TODO("Add proper error message")
        }
    }

    // TODO which one should be overridden? this one or with 2 parameters?
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        if (deserializer.descriptor == byteArraySerializer.descriptor)
            decodeByteArray() as T
        else
            super.decodeSerializableValue(deserializer)
}
