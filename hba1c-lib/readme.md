## hba1c-lib

A C shared library exposing two functions:
- `calculate_hba1c_pure(measurements, length)` — HbA1c % from an array of glucose readings (mmol/L)
- `calculate_iob(dose, time_since_inj, dia)` — Insulin on Board from a linear decay model

### Build via Docker (recommended)

```
docker compose up hba1c-builder
```

The compiled `libdiabetes.so` is written to `./hba1c-lib/dist/` via bind mount.
The backend service mounts the same directory at `/usr/local/lib/nativelibs/`.

### Build locally (Linux / WSL)

```bash
mkdir -p hba1c-lib/dist
gcc -shared -fPIC -O2 -Ihba1c-lib/include -o hba1c-lib/dist/libdiabetes.so hba1c-lib/src/*.c -lm
```

### Build locally (Windows — .dll)

```bash
gcc -shared -O2 -Ihba1c-lib/include -o hba1c-lib/dist/diabetes.dll hba1c-lib/src/*.c
```

### Using in Kotlin via JNA

JNA is already configured in the backend. Inject `HbA1cLibrary` wherever needed:

```kotlin
@Service
class GlucoseService(private val lib: HbA1cLibrary) {
    fun getHba1c(readings: DoubleArray): Double =
        lib.calculate_hba1c_pure(readings, NativeLong(readings.size.toLong()))

    fun getIob(dose: Double, minutesElapsed: Double, diaMinutes: Double): Double =
        lib.calculate_iob(dose, minutesElapsed, diaMinutes)
}
```
