package com.marcowong.motoman.track

class TrackSegment {
    @JvmField var prev: TrackSegment? = null
    @JvmField var next: TrackSegment? = null
    @JvmField var x1: Float = 0f
    @JvmField var y1: Float = 0f
    @JvmField var l1: Float = 0f
    @JvmField var r1: Float = 0f
    @JvmField var w1: Float = 0f
    @JvmField var x2: Float = 0f
    @JvmField var y2: Float = 0f
    @JvmField var l2: Float = 0f
    @JvmField var r2: Float = 0f
    @JvmField var w2: Float = 0f
    @JvmField var attributes: MutableMap<String, Any> = mutableMapOf()
}
