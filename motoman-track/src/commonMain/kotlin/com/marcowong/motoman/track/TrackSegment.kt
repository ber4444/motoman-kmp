package com.marcowong.motoman.track

class TrackSegment {
    var prev: TrackSegment? = null
    var next: TrackSegment? = null
    var x1: Float = 0f
    var y1: Float = 0f
    var l1: Float = 0f
    var r1: Float = 0f
    var w1: Float = 0f
    var x2: Float = 0f
    var y2: Float = 0f
    var l2: Float = 0f
    var r2: Float = 0f
    var w2: Float = 0f
    var attributes: MutableMap<String, Any> = mutableMapOf()
}
