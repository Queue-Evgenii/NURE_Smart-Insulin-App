#include "../include/diabetes.h"
#include <math.h>

/**
 * Calculates the amount of Insulin on Board (IOB) based on a linear decay model.
 * * @param dose           The initial insulin dose administered (Units).
 * @param time_since_inj Time elapsed since the injection was given (minutes).
 * @param dia            Duration of Insulin Action (minutes). 
 * Typically ranges from 180 to 300 minutes depending on the insulin type.
 * * @return The remaining active insulin (Units) still present in the body, 
 * or 0.0 if the time elapsed exceeds the duration of action.
 */
EXPORT double calculate_iob(double dose, double time_since_inj, double dia) {
    if (time_since_inj <= 0) return dose;
    if (time_since_inj >= dia) return 0.0;

    // IOB = Dose * (1 - (t/DIA))
    double iob = dose * (1.0 - (time_since_inj / dia));
    
    return (iob < 0) ? 0 : iob;
}