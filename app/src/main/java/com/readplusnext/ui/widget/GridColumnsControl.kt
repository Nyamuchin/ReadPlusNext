package com.readplusnext.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.readplusnext.domain.model.AppSettings
import androidx.compose.material3.ButtonGroup

/** Compact 2–5 column picker shared by the home layout sheet and the settings screen. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GridColumnsControl(
    comicColumns: Int,
    videoColumns: Int,
    onComicColumnsChange: (Int) -> Unit,
    onVideoColumnsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ColumnsRow(
            label = "漫画列数",
            description = "首页漫画标签页每行显示的数量",
            columns = comicColumns,
            onChange = onComicColumnsChange,
        )
        ColumnsRow(
            label = "视频列数",
            description = "首页视频标签页每行显示的数量",
            columns = videoColumns,
            onChange = onVideoColumnsChange,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnsRow(
    label: String,
    description: String,
    columns: Int,
    onChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ButtonGroup(
                expandedRatio = 0.12f,
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                val values = (AppSettings.MIN_GRID_COLUMNS..AppSettings.MAX_GRID_COLUMNS).toList()
                values.forEachIndexed { index, value ->
                    val shapes = when {
                        values.size == 1 -> ToggleButtonDefaults.shapes()
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == values.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = columns == value,
                        onCheckedChange = { onChange(value) },
                        shapes = shapes,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) {
                        Text(text = "$value")
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (columns == AppSettings.DEFAULT_GRID_COLUMNS) "默认" else "自定义",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
