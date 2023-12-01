# kotlinx-serialization-bencode

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/knok16/kotlinx-serialization-bencode/tree/master.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/knok16/kotlinx-serialization-bencode/tree/master)

[Bencode](https://en.wikipedia.org/wiki/Bencode) format
for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).

## Table of contents

<!--- TOC -->

* [Full Example](#full-example)
* [Supported types](#supported-types)
    * [Byte String](#byte-strings)
    * [Integers](#integers)
    * [Lists](#lists)
    * [Dictionaries](#dictionaries)
    * [BencodeElement](#bencodeelement)
    * [Nesting and composition](#nesting-and-composition)
* [Bencode Configuration](#bencode-configuration)
    * [Ignore Unknown Keys](#ignore-unknown-keys)

<!--- END -->

## Full Example

```kotlin
package com.github.knok16.bencode

import com.github.knok16.bencode.Bencode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import java.io.File

@Serializable
data class TorrentMetadata(
    val announce: String,
    val publisher: String? = null,
    @SerialName("creation date")
    val creationDate: Long,
    @SerialName("created by")
    val createdBy: String,
    val encoding: String? = null,
    val comment: String?,
    @SerialName("announce-list")
    val announceList: List<List<String>>,
    @SerialName("publisher-url")
    val publisherUrl: String? = null,
    val info: Info
)

@Serializable
data class Info(
    val private: Long = 0,
    val length: Long,
    val pieces: ByteArray,
    @SerialName("piece length")
    val pieceLength: Long,
    val name: String
)

fun main() {
    val bytes = File("bencoded-file").readBytes()

    val data = Bencode.decodeFromByteArray<TorrentMetadata>(bytes)

    println(data)
}
```

## Supported types

### Byte Strings

One of basic Bencode elements is string of bytes, that can be parsed into either `ByteArray` or `String`:

```kotlin
val bytes = "11:byte-string".toByteArray()

println(
    Bencode.decodeFromByteArray<ByteArray>(bytes).contentToString()
) // [98, 121, 116, 101, 45, 115, 116, 114, 105, 110, 103]
println(Bencode.decodeFromByteArray<String>(bytes)) // byte-string
```

To parse `String` from bytes `UTF-8` encoding is used:

```kotlin
val bytes = "8:§§§§".toByteArray(charset = Charsets.UTF_8)

println(Bencode.decodeFromByteArray<String>(bytes)) // §§§§
```

### Integers

Another basic Bencode elements is integers, that can be parsed into:

- `Byte`
- `Short`
- `Char`
- `Int`
- `Long`

```kotlin
val bytes = "i64e".toByteArray()

println(Bencode.decodeFromByteArray<Byte>(bytes)) // 64
println(Bencode.decodeFromByteArray<Short>(bytes)) // 64
println(Bencode.decodeFromByteArray<Char>(bytes)) // @
println(Bencode.decodeFromByteArray<Int>(bytes)) // 64
println(Bencode.decodeFromByteArray<Long>(bytes)) // 64
```

### Lists

Bencoded Lists can be parsed into:

- `List<T>`
- `Array<T>`
- `ShortArray`
- `CharArray`
- `IntArray`
- `LongArray`

```kotlin
val bytes = "l3:foo3:bare".toByteArray()

println(Bencode.decodeFromByteArray<List<String>>(bytes)) // [foo, bar]
println(Bencode.decodeFromByteArray<Array<String>>(bytes).contentToString()) // [foo, bar]
```

```kotlin
val bytes = "li78ei79ei73ei67ei69ee".toByteArray()

println(Bencode.decodeFromByteArray<ShortArray>(bytes).contentToString()) // [78, 79, 73, 67, 69]
println(Bencode.decodeFromByteArray<CharArray>(bytes).contentToString()) // [N, O, I, C, E]
println(Bencode.decodeFromByteArray<IntArray>(bytes).contentToString()) // [78, 79, 73, 67, 69]
println(Bencode.decodeFromByteArray<LongArray>(bytes).contentToString()) // [78, 79, 73, 67, 69]
```

### Dictionaries

Bencoded Dictionaries can be parsed into:

- `Map<BencodeString, V>`
- `Map<String, V>`
- `Map<ByteArray, V>` - be careful with such types, as it is usually bad practice to use raw arrays as map's key
- POJOs - names of fields will be assumed to be UTF-8 encoded

```kotlin
val bytes = "d3:abc3:def3:foo3:bare".toByteArray()

println(Bencode.decodeFromByteArray<Map<String, String>>(bytes)) // {abc=def, foo=bar}
println(Bencode.decodeFromByteArray<Map<BencodeString, String>>(bytes)) // {abc=def, foo=bar}
println(Bencode.decodeFromByteArray<Map<ByteArray, String>>(bytes)) // {[B@11531931=def, [B@5e025e70=bar}

@Serializable
data class POJO(
    val abc: String,
    val foo: String
)

println(Bencode.decodeFromByteArray<POJO>(bytes)) // POJO(abc=def, foo=bar)
```

### BencodeElement

kotlinx-serialization-bencode provides simple classes to parse bencode data into:

- `BencodeString`
- `BencodeNumber`
- `BencodeList`
- `BencodeDictionary`

### Nesting and composition

All previously mentioned types can be nested and composed to create new types, that still be parsable:

```kotlin
@Serializable
data class R(
    val name: String,
    val inner: R? = null
)

val bytes = "d4:name5:alice5:innerd4:name3:bobee".toByteArray()

println(Bencode.decodeFromByteArray<R>(bytes)) // R(name=alice, inner=R(name=bob, inner=null))

@Serializable
data class A(
    val stringField: String,
    val list: List<Map<String, R>>,
    val byteField: Byte
)

val bytes = "d9:byteFieldi12e11:stringField3:foo4:listlded3:bard4:name5:alice5:innerd4:name3:bobeeedeee".toByteArray()

println(Bencode.decodeFromByteArray<A>(bytes)) // A(stringField=foo, list=[{}, {bar=R(name=alice, inner=R(name=bob, inner=null))}, {}], byteField=12)
```

## Bencode Configuration

kotlinx-serialization-bencode provide a number of different configurations

### Ignore Unknown Keys

By default, decoding will throw an exception if unknown field name met in serialized data,
to instruct decoder to ignore such properties `ignoreUnknownKeys = true` Bencode parameter can be used:

```kotlin
data class IgnoreUnknownKeysExample(
    val knownProperty: String
)

val bytes = "d13:knownProperty3:foo15:unknownProperty3:bare".toByteArray()

val result = Bencode {
    ignoreUnknownKeys = true
}.decodeFromByteArray<IgnoreUnknownKeysExample>(bytes)

println(result) // IgnoreUnknownKeysExample(knownProperty=foo)
```
