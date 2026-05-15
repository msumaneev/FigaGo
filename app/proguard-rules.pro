# ProGuard rules для FigaGo

# Оставляем модели данных и сущности Room
-keep class com.figago.data.entity.** { *; }
-keep class com.figago.domain.model.** { *; }
-keep class com.figago.ui.main.DayState { *; }

# Оставляем ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Оставляем все перечисления (Enums)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
