package com.github.knok16.serialization

import com.github.knok16.bencode.Reader
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

// TODO add opt-ins
sealed class Bencode(
    internal val ignoreUnknownKeys: Boolean,
    override val serializersModule: SerializersModule
) : BinaryFormat {
    companion object Default : Bencode(false, EmptySerializersModule())

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val decoder = BencodeDecoder(this, Reader(bytes))
        return decoder.decodeSerializableValue(deserializer)
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        TODO("Not yet implemented")
    }
}

private class BencodeImpl(builder: BencodeBuilder) :
    Bencode(builder.ignoreUnknownKeys, builder.serializersModule)

/**
 * Creates an instance of [Bencode] configured from the optionally given [Bencode instance][from]
 * and adjusted with [builderAction].
 */
fun Bencode(from: Bencode = Bencode, builderAction: BencodeBuilder.() -> Unit): Bencode =
    BencodeImpl(BencodeBuilder(from).apply(builderAction))

/**
 * Builder of the [Bencode] instance provided by `Bencode` factory function.
 */
class BencodeBuilder internal constructor(bencode: Bencode) {
    /**
     * Specifies whether encounters of unknown properties in the input Bencode
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    var ignoreUnknownKeys: Boolean = bencode.ignoreUnknownKeys

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Bencode] instance.
     */
    var serializersModule: SerializersModule = bencode.serializersModule
}
