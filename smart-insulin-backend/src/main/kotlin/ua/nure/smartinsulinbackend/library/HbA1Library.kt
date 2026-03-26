package ua.nure.smartinsulinbackend.library

import com.sun.jna.Library
import com.sun.jna.NativeLong

interface HbA1cLibrary : Library {
    // length maps to C size_t (8 bytes on 64-bit Linux)
    fun calculate_hba1c_pure(measurements: DoubleArray, length: NativeLong): Double
    fun calculate_iob(dose: Double, timeSinceInj: Double, dia: Double): Double
}