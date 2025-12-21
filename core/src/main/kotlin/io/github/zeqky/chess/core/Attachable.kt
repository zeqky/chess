package io.github.zeqky.chess.core

abstract class Attachable {
    private var attachment: Any? = null

    fun <T: Any> attach(instance: T) {
        this.attachment = instance as Any
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> attachment(): T {
        return attachment as T
    }
}