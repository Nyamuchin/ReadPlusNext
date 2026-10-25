package com.readplusnext.domain.model

import com.readplusnext.domain.model.ReadingMode
import com.readplusnext.domain.model.ThemeMode
import com.readplusnext.domain.model.ThemeStyle

data class AppSettings(
    val themeStyle: ThemeStyle = ThemeStyle.MATERIAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val readingMode: ReadingMode = ReadingMode.LEFT_TO_RIGHT,
    val comicGridColumns: Int = DEFAULT_GRID_COLUMNS,
    val videoGridColumns: Int = DEFAULT_GRID_COLUMNS,
) {
    companion object {
        const val DEFAULT_GRID_COLUMNS = 3
        const val MIN_GRID_COLUMNS = 2
        const val MAX_GRID_COLUMNS = 5

        fun clampColumns(columns: Int): Int = columns.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }
}
