package com.marcowong.motoman.track.logic

interface IMotorcycleInputMeters {
    fun getEngineAndBrakeMeter(): Float
    fun getCounterSteeringMeter(): Float
    fun getLeanMeter(): Float
    fun setMotorcycle(motorcycle: Motorcycle)
}
