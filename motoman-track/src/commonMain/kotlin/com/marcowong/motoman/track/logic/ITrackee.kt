package com.marcowong.motoman.track.logic

import com.marcowong.motoman.track.TrackSegment
import com.marcowong.motoman.track.math.Vector3

interface ITrackee {
    fun getTrackeePos(vec: Vector3)
    fun setLastTrackSegment(ts: TrackSegment?)
    fun getLastTrackSegment(): TrackSegment?
}
