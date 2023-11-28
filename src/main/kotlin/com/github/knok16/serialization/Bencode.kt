package com.github.knok16.serialization

import com.github.knok16.bencode.Reader
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

// TODO add opt-ins
sealed class Bencode(
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : BinaryFormat {
    companion object Default : Bencode()

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val decoder = BencodeDecoder(Reader(bytes), serializersModule)
        return decoder.decodeSerializableValue(deserializer)
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        TODO("Not yet implemented")
    }
}
