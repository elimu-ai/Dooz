/*
 *     Dooz
 *     ThemeSettings.kt Created/Updated by Yamin Siahmargooei at 2022/9/22
 *     This file is part of Dooz.
 *     Copyright (C) 2022  Yamin Siahmargooei
 *
 *     Dooz is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Dooz is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Dooz.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.yamin8000.dooz.content.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ThemeSetting(
    val persianName: String
) : Parcelable {
    Dark("تاریک"), Light("روشن"), System("هماهنگ سیستم")
}