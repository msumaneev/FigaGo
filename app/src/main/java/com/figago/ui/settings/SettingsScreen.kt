package com.figago.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_DISTANCE
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_OFF
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_TIME
import com.figago.domain.repository.SettingsRepository.Companion.UNIT_KILOMETERS
import com.figago.domain.repository.SettingsRepository.Companion.UNIT_MILES
import com.figago.ui.components.LabeledSettingRow
import com.figago.ui.components.WheelNumberPickerCore
import com.figago.ui.components.WheelNumberPickerSetting
import com.figago.domain.model.BatteryWeightProfiles
import com.figago.R

// --- ЕДИНЫЕ ПЕРЕМЕННЫЕ ДЛЯ ШРИФТОВ НАСТРОЕК ---
private val TextSizeSectionTitle = 22.sp
private val TextSizeItemTitle = 18.sp
private val TextSizeItemDesc = 14.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    mainViewModel: com.figago.ui.main.MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val sessionCount by viewModel.activeProfileSessionCount.collectAsStateWithLifecycle()
    val profiles by mainViewModel.profiles.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ===== Паспорт коляски (текущий профиль) =====
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SectionHeader removed

                    val iconOptions = listOf(
                        R.drawable.ic_profile_man to stringResource(R.string.profile_icon_man),
                        R.drawable.ic_profile_biker to stringResource(R.string.profile_icon_biker),
                        R.drawable.ic_profile_scooter to stringResource(R.string.profile_icon_scooter),
                        R.drawable.ic_wheelchair_permobil_f to "Permobil F",
                        R.drawable.ic_wheelchair_permobil_m to "Permobil M",
                        R.drawable.ic_wheelchair_optimus to "Optimus",
                        R.drawable.ic_wheelchair_exotic to stringResource(R.string.profile_icon_exotic),
                        R.drawable.ic_wheelchair_manual to stringResource(R.string.profile_type_manual)
                    )

                    // Иконка и Название
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        // Компактный выбор иконки (слева)
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.width(80.dp)
                        ) {
                            val defaultIcon = if (activeProfile?.type == com.figago.data.entity.ProfileType.ELECTRIC) R.drawable.ic_profile_man else R.drawable.ic_wheelchair_manual
                            val currentIconId = activeProfile?.iconId ?: 0
                            val safeIconId = if (currentIconId != 0) currentIconId else defaultIcon
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.profile_icon_label), maxLines = 1) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                leadingIcon = {
                                    Icon(painter = painterResource(id = safeIconId), contentDescription = null, modifier = Modifier.size(24.dp))
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                iconOptions.forEach { (id, label) ->
                                    DropdownMenuItem(
                                        text = { 
                                            Icon(painter = painterResource(id = id), contentDescription = label, modifier = Modifier.size(32.dp)) 
                                        },
                                        onClick = {
                                            viewModel.updateProfile { it.copy(iconId = id) }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Название (занимает всё оставшееся пространство)
                        OutlinedTextField(
                            value = activeProfile?.name ?: "",
                            onValueChange = { newName -> viewModel.updateProfile { it.copy(name = newName) } },
                            label = null,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Электрическая / Ручная
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = activeProfile?.type == com.figago.data.entity.ProfileType.ELECTRIC,
                            onClick = { viewModel.updateProfile { it.copy(type = com.figago.data.entity.ProfileType.ELECTRIC) } },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text(stringResource(R.string.profile_type_electric)) }
                        SegmentedButton(
                            selected = activeProfile?.type == com.figago.data.entity.ProfileType.MANUAL,
                            onClick = { viewModel.updateProfile { it.copy(type = com.figago.data.entity.ProfileType.MANUAL) } },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text(stringResource(R.string.profile_type_manual)) }
                    }

                    val speedSuffix = if (state.unitSystem == UNIT_MILES) stringResource(R.string.unit_mph) else stringResource(R.string.unit_kmh)
                    val distSuffix = if (state.unitSystem == UNIT_MILES) stringResource(R.string.unit_mi) else stringResource(R.string.unit_km)

                    WheelNumberPickerSetting(
                        title = stringResource(R.string.profile_max_speed),
                        description = stringResource(R.string.profile_max_speed_desc),
                        value = activeProfile?.maxSpeed?.toInt() ?: 10,
                        range = 1..25,
                        onValueChange = { speed -> viewModel.updateProfile { it.copy(maxSpeed = speed.toFloat()) } },
                        suffix = speedSuffix
                    )

                    SwitchSetting(
                        title = stringResource(com.figago.R.string.settings_auto_transport_title),
                        description = stringResource(com.figago.R.string.settings_auto_transport_desc),
                        checked = state.autoTransportDetectionEnabled,
                        onCheckedChange = viewModel::setAutoTransportDetectionEnabled,
                    )

                }
            }

            // ===== Батарея (только для электрической) =====
            if (activeProfile?.type == com.figago.data.entity.ProfileType.ELECTRIC) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionHeader(text = stringResource(R.string.settings_section_battery))

                        val batDistSuffix = if (state.unitSystem == UNIT_MILES) stringResource(R.string.unit_mi) else stringResource(R.string.unit_km)
                        val currentLedCount = activeProfile?.ledCount ?: 5
                        val currentMaxMileage = activeProfile?.maxMileage ?: 20f

                        // 1. Лампочки на пульте (dropdown с фиксированными значениями)
                        var ledDropdownExpanded by remember { mutableStateOf(false) }
                        val allowedLedCounts = BatteryWeightProfiles.ALLOWED_LED_COUNTS

                        LabeledSettingRow(
                            title = stringResource(R.string.profile_led_title),
                            description = stringResource(R.string.profile_led_desc),
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = ledDropdownExpanded,
                                onExpandedChange = { ledDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = "$currentLedCount",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.width(100.dp).menuAnchor(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ledDropdownExpanded) },
                                    singleLine = true,
                                )
                                ExposedDropdownMenu(
                                    expanded = ledDropdownExpanded,
                                    onDismissRequest = { ledDropdownExpanded = false }
                                ) {
                                    allowedLedCounts.forEach { count ->
                                        DropdownMenuItem(
                                            text = { Text("$count") },
                                            onClick = {
                                                val newDistances = BatteryWeightProfiles.calculateDistancesWithDelta(currentMaxMileage, emptyList(), count)
                                                viewModel.updateProfile { it.copy(ledCount = count, ledDistances = newDistances) }
                                                ledDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Пробег на одном заряде (общий)
                        WheelNumberPickerSetting(
                            title = stringResource(R.string.profile_range_title),
                            description = if (state.unitSystem == UNIT_MILES) stringResource(R.string.profile_range_desc_mi) else stringResource(R.string.profile_range_desc_km),
                            actionIcon = {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        val newDistances = BatteryWeightProfiles.calculateDistancesWithDelta(currentMaxMileage, emptyList(), currentLedCount)
                                        viewModel.updateProfile { it.copy(ledDistances = newDistances) }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                        contentDescription = "Reset Distances",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            value = currentMaxMileage.toInt(),
                            range = 5..99,
                            onValueChange = { miles ->
                                val newMileage = miles.toFloat()
                                viewModel.updateProfile { dbProfile ->
                                    val newDistances = BatteryWeightProfiles.calculateDistancesWithDelta(newMileage, emptyList(), dbProfile.ledCount ?: 5)
                                    dbProfile.copy(maxMileage = newMileage, ledDistances = newDistances)
                                }
                            },
                            suffix = batDistSuffix
                        )

                        // 3. Индивидуальные барабаны для каждой лампочки
                        val currentDistances = activeProfile?.ledDistances
                            ?: BatteryWeightProfiles.calculateDistancesWithDelta(currentMaxMileage, emptyList(), currentLedCount)

                        // Статистика по лампочкам
                        val lampAverages by viewModel.lampAverages.collectAsStateWithLifecycle()

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in 0 until currentLedCount) {
                                    val lampDist = currentDistances.getOrElse(i) { 0f }
                                    val avgStat = lampAverages[i]
                                    val statText = if (avgStat != null) {
                                        String.format(stringResource(R.string.lamp_stat_fact), avgStat)
                                    } else {
                                        stringResource(R.string.lamp_stat_no_data)
                                    }
                                    // Цвет статистики: оранжевый если отклонение > 20%
                                    val statColor = if (avgStat != null && lampDist > 0 && avgStat < lampDist * 0.8f) {
                                        Color(0xFFFB8C00) // оранжевый
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    }

                                    // На Дашборде лампа 1 - красная, лампа 5 - зеленая.
                                    // У нас i=0 это зеленая (последняя), i=4 это красная (первая).
                                    // Значит номер лампы: (currentLedCount - i)
                                    val lampNumber = currentLedCount - i
                                    
                                    WheelNumberPickerSetting(
                                        title = String.format(stringResource(R.string.lamp_number_title), lampNumber),
                                        description = statText,
                                        descriptionColor = statColor,
                                        value = lampDist.toInt(),
                                        range = 1..99,
                                        onValueChange = { newVal ->
                                            viewModel.updateProfile { dbProfile ->
                                                val dbDistances = dbProfile.ledDistances ?: BatteryWeightProfiles.calculateDistancesWithDelta(dbProfile.maxMileage ?: 20f, emptyList(), dbProfile.ledCount ?: 5)
                                                val updated = dbDistances.toMutableList()
                                                if (i < updated.size) {
                                                    updated[i] = newVal.toFloat()
                                                }
                                                val newTotal = BatteryWeightProfiles.calculateTotalMileage(updated)
                                                dbProfile.copy(maxMileage = newTotal, ledDistances = updated)
                                            }
                                        },
                                        suffix = batDistSuffix,
                                        titleIcon = {
                                            // На Дашборде генерируется так: fraction = (lampNumber - 1) / (maxCount - 1)
                                            val fraction = if (currentLedCount <= 1) 1f else (lampNumber - 1).toFloat() / (currentLedCount - 1)
                                            val color = when {
                                                fraction < 0.33f -> Color(0xFFE53935) // красный
                                                fraction < 0.66f -> Color(0xFFFB8C00) // оранжевый
                                                else -> Color(0xFF43A047) // зелёный
                                            }
                                            Box(modifier = Modifier
                                                .size(12.dp)
                                                .background(color, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== Настройки отображения =====
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(text = stringResource(R.string.settings_section_display))

                    UnitSystemSetting(
                        currentUnit = state.unitSystem,
                        onUnitChange = viewModel::setUnitSystem 
                    )
                }
            }



            // ===== Голосовые команды =====
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(text = stringResource(com.figago.R.string.settings_voice_assistant_section))

                    SwitchSetting(
                        title = stringResource(com.figago.R.string.settings_voice_vibrate),
                        description = null,
                        checked = state.vibrateOnCommand,
                        onCheckedChange = viewModel::setVibrateOnCommand,
                    )

                    SwitchSetting(
                        title = stringResource(com.figago.R.string.settings_voice_sound),
                        description = null,
                        checked = state.soundOnCommand,
                        onCheckedChange = viewModel::setSoundOnCommand,
                    )
                }
            }

            // ===== TTS-оповещения =====
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(text = stringResource(com.figago.R.string.settings_tts_section))

                    TtsModeSetting(
                        currentMode = state.ttsMode,
                        currentModeLabel = state.ttsModeLabel,
                        onModeChange = viewModel::setTtsMode,
                    )

                    if (state.ttsMode == TTS_MODE_DISTANCE) {
                        WheelNumberPickerSetting(
                            title = stringResource(com.figago.R.string.settings_tts_interval_km_title),
                            description = stringResource(com.figago.R.string.settings_tts_desc),
                            value = state.ttsDistanceIntervalKm.toInt(),
                            range = 1..99,
                            onValueChange = { viewModel.setTtsDistanceInterval(it.toDouble()) },
                            suffix = if (state.unitSystem == UNIT_MILES) stringResource(R.string.unit_mi) else stringResource(com.figago.R.string.settings_suffix_km),
                        )
                    }

                    if (state.ttsMode == TTS_MODE_TIME) {
                        WheelNumberPickerSetting(
                            title = stringResource(com.figago.R.string.settings_tts_interval_min_title),
                            description = stringResource(com.figago.R.string.settings_tts_desc),
                            value = state.ttsTimeIntervalMin,
                            range = 1..99,
                            onValueChange = viewModel::setTtsTimeInterval,
                            suffix = stringResource(com.figago.R.string.settings_suffix_min),
                        )
                    }
                }
            }

            // ===== Язык (последний пункт) =====
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(text = stringResource(com.figago.R.string.settings_language_title))
                    Text(
                        text = stringResource(com.figago.R.string.settings_language_desc),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    val langs = listOf(
                        "" to com.figago.R.string.lang_system,
                        "en" to com.figago.R.string.lang_en,
                        "ru" to com.figago.R.string.lang_ru,
                        "zh-rCN" to com.figago.R.string.lang_zh_cn,
                        "zh-rTW" to com.figago.R.string.lang_zh_tw,
                        "ko" to com.figago.R.string.lang_ko,
                        "ja" to com.figago.R.string.lang_ja,
                        "tr" to com.figago.R.string.lang_tr,
                        "ar" to com.figago.R.string.lang_ar,
                        "ka" to com.figago.R.string.lang_ka,
                        "uk" to com.figago.R.string.lang_uk,
                        "es" to com.figago.R.string.lang_es,
                        "fr" to com.figago.R.string.lang_fr,
                        "de" to com.figago.R.string.lang_de
                    )
                    val currentLangName = langs.find { it.first == state.appLanguageCode }?.second ?: com.figago.R.string.lang_system

                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(currentLangName), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            langs.forEach { (code, nameRes) ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(nameRes), color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        viewModel.setAppLanguage(code)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Удалить профиль (если больше 1)
            if (profiles.size > 1) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_delete_profile))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== Версия приложения =====
            Text(
                text = "FigaGo v${com.figago.BuildConfig.VERSION_NAME} (${com.figago.BuildConfig.VERSION_CODE})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // ===== Ссылка на обратную связь =====
            var showTelemetryDialog by remember { mutableStateOf(false) }

            Text(
                text = stringResource(R.string.settings_contact_developer),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTelemetryDialog = true }
                    .padding(vertical = 8.dp)
            )

            if (showTelemetryDialog) {
                TelemetryExportDialog(
                    onDismiss = { showTelemetryDialog = false },
                    onExport = { categories ->
                        showTelemetryDialog = false
                        viewModel.exportTelemetry(categories)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showDeleteDialog && activeProfile != null) {
            val toDeleteId = activeProfile!!.id
            var reassignMode by remember { mutableStateOf(false) }
            var selectedReassignId by remember { mutableStateOf<Long?>(null) }
            
            LaunchedEffect(reassignMode) {
                if (reassignMode && selectedReassignId == null) {
                    selectedReassignId = profiles.firstOrNull { it.id != toDeleteId }?.id
                }
            }

            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_profile_title, activeProfile!!.name)) },
                text = {
                    Column {
                        if (sessionCount == 0) {
                            Text(stringResource(R.string.delete_profile_confirm_simple))
                        } else {
                            Text(stringResource(R.string.delete_profile_confirm_complex, sessionCount))
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = !reassignMode, onClick = { reassignMode = false })
                                Text(stringResource(R.string.delete_profile_action_delete), modifier = Modifier.clickable { reassignMode = false })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = reassignMode, onClick = { reassignMode = true })
                                Text(stringResource(R.string.delete_profile_action_reassign), modifier = Modifier.clickable { reassignMode = true })
                            }
                            if (reassignMode) {
                                Spacer(Modifier.height(8.dp))
                                val otherProfiles = profiles.filter { it.id != toDeleteId }
                                var exp by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
                                    OutlinedTextField(
                                        value = otherProfiles.find { it.id == selectedReassignId }?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) }
                                    )
                                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                        otherProfiles.forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text(p.name) },
                                                onClick = {
                                                    selectedReassignId = p.id
                                                    exp = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val reassignId = if (sessionCount > 0 && reassignMode) selectedReassignId else null
                        viewModel.deleteProfile(toDeleteId, reassignId) {
                            showDeleteDialog = false
                            onNavigateBack() // Go back after deletion
                        }
                    }) {
                        Text(stringResource(R.string.delete_profile_title, "").trim(), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
    }
}

// ===== Вспомогательные компоненты =====

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = TextSizeSectionTitle,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                fontSize = TextSizeItemTitle,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Text(
                    text = description,
                    fontSize = TextSizeItemDesc,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// Wheel pickers are now in com.figago.ui.components.WheelNumberPicker.kt

@Composable
private fun TtsModeSetting(
    currentMode: String,
    currentModeLabel: String,
    onModeChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${stringResource(com.figago.R.string.settings_tts_mode_label)}: $currentModeLabel", 
            fontSize = TextSizeItemTitle,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val modes = listOf(
                TTS_MODE_OFF to stringResource(com.figago.R.string.settings_tts_off_short),
                TTS_MODE_DISTANCE to stringResource(R.string.settings_tts_by_km),
                TTS_MODE_TIME to stringResource(R.string.settings_tts_by_min),
            )
            for ((mode, label) in modes) {
                val isSelected = currentMode == mode
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    onClick = { onModeChange(mode) },
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = TextSizeItemTitle,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitSystemSetting(
    currentUnit: String,
    onUnitChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_unit_title), 
            fontSize = TextSizeItemTitle,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.settings_unit_desc),
            fontSize = TextSizeItemDesc,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val units = listOf(
                UNIT_KILOMETERS to stringResource(R.string.settings_unit_km),
                UNIT_MILES to stringResource(R.string.settings_unit_mi),
            )
            for ((unit, label) in units) {
                val isSelected = currentUnit == unit
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    onClick = { onUnitChange(unit) },
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = TextSizeItemTitle,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
