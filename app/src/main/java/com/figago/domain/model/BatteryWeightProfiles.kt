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
     * Рассчитывает пробег каждой лампочки на основе общего пробега и весов.
     *
     * @param maxMileage общий пробег на одном заряде (км)
     * @param ledCount количество лампочек
     * @return список пробегов для каждой лампочки (км)
     */
    fun calculateDistances(maxMileage: Float, ledCount: Int): List<Float> {
        val weights = getWeights(ledCount)
        return weights.map { weight -> maxMileage * weight }
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
