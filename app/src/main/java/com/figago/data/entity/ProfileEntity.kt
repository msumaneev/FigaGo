package com.figago.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Тип профиля коляски.
 */
enum class ProfileType {
    ELECTRIC,
    MANUAL
}

/**
 * Профиль инвалидной коляски (Паспорт).
 *
 * @property id           Уникальный идентификатор.
 * @property type         Тип коляски (Электрическая / Ручная).
 * @property name         Название профиля (например: "Моя электрическая").
 * @property iconId       ID ресурса иконки.
 * @property maxSpeed     Максимальная скорость коляски (в км/ч).
 * @property maxMileage   Пробег на полном заряде (км, может быть null для ручной).
 * @property ledCount     Количество индикаторов (лампочек) батареи на пульте (может быть null).
 * @property ledDistances Настроенные пороги пробега для каждой лампочки в метрах (хранятся в JSON).
 */
@Entity(tableName = "profile")
data class ProfileEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: ProfileType = ProfileType.ELECTRIC,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon_id")
    val iconId: Int,

    @ColumnInfo(name = "max_speed")
    val maxSpeed: Float,

    @ColumnInfo(name = "max_mileage")
    val maxMileage: Float? = null,

    @ColumnInfo(name = "led_count")
    val ledCount: Int? = null,

    @ColumnInfo(name = "led_distances")
    val ledDistances: List<Float>? = null
)
