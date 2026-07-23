package com.marcowong.motoman.model

/**
 * Growable primitive lists, replacing libGDX's `FloatArray`/`IntArray` utility types.
 * Unboxed on purpose: the OBJ loader pushes hundreds of thousands of values and
 * `ArrayList<Float>` would box every one of them.
 */
class FloatList(initialCapacity: Int = 16) {
    private var items = FloatArray(initialCapacity.coerceAtLeast(1))
    var size: Int = 0
        private set

    fun add(value: Float) {
        if (size == items.size) items = items.copyOf(items.size * 2)
        items[size++] = value
    }

    operator fun get(index: Int): Float = items[index]

    fun clear() { size = 0 }
}

class IntList(initialCapacity: Int = 16) {
    private var items = IntArray(initialCapacity.coerceAtLeast(1))
    var size: Int = 0
        private set

    fun add(value: Int) {
        if (size == items.size) items = items.copyOf(items.size * 2)
        items[size++] = value
    }

    operator fun get(index: Int): Int = items[index]

    fun clear() { size = 0 }
}
