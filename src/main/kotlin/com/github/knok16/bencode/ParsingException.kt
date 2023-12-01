package com.github.knok16.bencode

import kotlinx.serialization.SerializationException

class ParsingException(reason: String, val at: Int? = null) : SerializationException(reason)