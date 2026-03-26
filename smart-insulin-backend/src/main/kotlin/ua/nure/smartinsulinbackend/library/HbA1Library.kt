package ua.nure.smartinsulinbackend.library

import com.sun.jna.Library

interface HbA1cLibrary : Library {
    fun calculate_hba1c_pure(measurements: DoubleArray, length: Int): Double
}