package com.figago.domain.model

/**
 * Профили весов батареи для разных типов пультов.
 *
 * Каждый массив содержит доли ёмкости батареи по лампочкам.
 * Индекс 0 — самая верхняя (первая тухнущая) лампа,
 * индекс N — нижняя (последняя, красная).
 *
 * Сумма элементов каждого массива ≈ 1.0
 */
object BatteryWeightProfiles {

    /** Допустимые значения количества лампочек. */
    val ALLOWED_LED_COUNTS = listOf(3, 4, 5, 6, 8, 10)

    private val TYPE_3  = floatArrayOf(0.45f, 0.35f, 0.20f)
    private val TYPE_4  = floatArrayOf(0.35f, 0.30f, 0.20f, 0.15f)
    private val TYPE_5  = floatArrayOf(0.30f, 0.20f, 0.20f, 0.15f, 0.15f)
    private val TYPE_6  = floatArrayOf(0.25f, 0.20f, 0.15f, 0.15f, 0.15f, 0.10f)
    private val TYPE_8  = floatArrayOf(0.20f, 0.15f, 0.15f, 0.12f, 0.12f, 0.12f, 0.09f, 0.05f)
    private val TYPE_10 = floatArrayOf(0.13f, 0.13f, 0.13f, 0.10f, 0.10f, 0.10f, 0.10f, 0.07f, 0.07f, 0.07f)

    /**
     * Возвращает массив весов для указанного количества лампочек.
     *
     * @param ledCount количество лампочек (3, 4, 5, 6, 8 или 10)
     * @return массив весов. Для неподдерживаемых значений — линейное распределение (fallback).
     */
    fun getWeights(ledCount: Int): FloatArray = when (ledCount) {
        3  -> TYPE_3
        4  -> TYPE_4
        5  -> TYPE_5
        6  -> TYPE_6
        8  -> TYPE_8
        10 -> TYPE_10
        else -> FloatArray(ledCount) { 1f / ledCount } // fallback: линейное распределение
    }

    /**
     * Рассчитывает новые дистанции при изменении общего пробега.
     * Если это первичный пересчет (старые дистанции пустые), использует веса.
     * Иначе вычисляет дельту и равномерно распределяет ее (с округлением) по текущим лампам.
     */
    fun calculateDistancesWithDelta(newMaxMileage: Float, currentDistances: List<Float>, ledCount: Int): List<Float> {
        if (currentDistances.isEmpty() || currentDistances.size != ledCount) {
            val weights = getWeights(ledCount)
            val exactDistances = weights.map { weight -> newMaxMileage * weight }
            val roundedDistances = exactDistances.map { kotlin.math.round(it) }.toMutableList()
            
            // Adjust to ensure the sum exactly matches the target newMaxMileage
            var diff = newMaxMileage.toInt() - roundedDistances.sum().toInt()
            var i = 0
            while (diff != 0) {
                if (diff > 0) {
                    roundedDistances[i % ledCount] += 1f
                    diff--
                } else {
                    if (roundedDistances[i % ledCount] > 0f) {
                        roundedDistances[i % ledCount] -= 1f
                        diff++
                    }
                }
                i++
            }
            return roundedDistances
        }

        val oldTotal = currentDistances.sum()
        val delta = newMaxMileage - oldTotal
        if (delta == 0f) return currentDistances

        val result = currentDistances.toMutableList()
        val deltaInt = delta.toInt()
        val baseDelta = deltaInt / ledCount
        var remainder = kotlin.math.abs(deltaInt % ledCount)

        for (i in result.indices) {
            result[i] += baseDelta.toFloat()
            if (remainder > 0) {
                result[i] += if (delta > 0) 1f else -1f
                remainder--
            }
        }
        
        // Prevent negative values
        for (i in result.indices) {
            if (result[i] < 0f) result[i] = 0f
        }
        return result
    }

    /**
     * Рассчитывает общий пробег из индивидуальных значений лампочек.
     *
     * @param ledDistances пробег каждой лампочки (км)
     * @return суммарный пробег
     */
    fun calculateTotalMileage(ledDistances: List<Float>): Float {
        return ledDistances.sum()
    }
}
