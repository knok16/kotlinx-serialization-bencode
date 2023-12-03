package com.github.knok16.bencode

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.nio.charset.Charset

// TODO add opt-ins
sealed class Bencode(
    internal val ignoreUnknownKeys: Boolean,
    internal val stringCharset: Charset,
    override val serializersModule: SerializersModule
) : BinaryFormat {
    companion object Default : Bencode(false, Charsets.UTF_8, EmptySerializersModule())

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val reader = Reader(bytes)

        val decoder = BencodeDecoder(this, reader)

        return decoder.decodeSerializableValue(deserializer).also {
            if (reader.peek() != null)
                reader.unexpectedToken("end of input")
        }
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        TODO("Not yet implemented")
    }
}

private class BencodeImpl(builder: BencodeBuilder) :
    Bencode(builder.ignoreUnknownKeys, builder.stringCharset, builder.serializersModule)

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
     * Charset used to decode/encode [String] from/to [ByteArray].
     * By default, [Charsets.UTF_8] encoding is used.
     */
    var stringCharset: Charset = bencode.stringCharset

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Bencode] instance.
     */
    var serializersModule: SerializersModule = bencode.serializersModule
}
