package com.figago.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LabeledSettingRow(
    title: String,
    description: String? = null,
    accentColor: Color? = null,
    descriptionColor: Color? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .height(108.dp) 
        ) {
            Box(
                modifier = Modifier.height(36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    color = accentColor ?: MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            if (description != null) {
                Box(
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = descriptionColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(108.dp)
        ) {
            content()
        }
    }
}

@Composable
fun WheelNumberPickerSetting(
    title: String,
    description: String? = null,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    suffix: String = "",
    accentColor: Color? = null,
    descriptionColor: Color? = null,
) {
    LabeledSettingRow(title, description, accentColor, descriptionColor) {
        WheelNumberPickerCore(
            value = value,
            range = range,
            onValueChange = onValueChange,
            accentColor = accentColor,
        )
            
        if (suffix.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = suffix, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelNumberPickerCore(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    accentColor: Color? = null,
    format: (Int) -> String = { it.toString() }
) {
    val itemHeight = 36.dp
    val items = range.toList()
    val initialIndex = items.indexOf(value).coerceAtLeast(0)
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Флаг: идёт программная прокрутка (не вызываем onValueChange)
    var isExternalScroll by remember { mutableStateOf(false) }

    // Прокрутка при внешнем изменении value (смена профиля)
    LaunchedEffect(value) {

        val targetIndex = items.indexOf(value).coerceAtLeast(0)

        if (targetIndex != listState.firstVisibleItemIndex) {

            isExternalScroll = true
            listState.scrollToItem(targetIndex)
            isExternalScroll = false
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->

                if (!isExternalScroll && index in items.indices) {
                    onValueChange(items[index])
                }
            }
    }

    Box(
        modifier = Modifier
            .width(50.dp)
            .height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(itemHeight)) }
            
            items(items.size) { index ->
                val v = items[index]
                val isSelected by remember { derivedStateOf { index == listState.firstVisibleItemIndex } }
                
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(v),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) (accentColor ?: MaterialTheme.colorScheme.onSurface) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = if (isSelected) 22.sp else 18.sp
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(itemHeight)) }
        }
    }
}
