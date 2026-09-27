package com.aliothmoon.maameow.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import com.aliothmoon.maameow.data.resource.ItemIconLoader

/**
 * 物品图标：按 id 取图，取不到（或还在取）时占一块同尺寸的空位。
 *
 * 取图是异步的，占位不能省 —— 少了它，图标落地那一刻整行会跳一下。
 * 各页的物品图标都走这一份，别各自再写一遍 produceState。
 */
@Composable
internal fun ItemIcon(
    itemId: String,
    contentDescription: String?,
    size: Dp,
    loader: ItemIconLoader,
) {
    val icon by produceState<ImageBitmap?>(initialValue = null, itemId) {
        value = loader.load(itemId)
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier
                .height(size)
                .width(size),
        )
    } else {
        Spacer(Modifier.size(size))
    }
}
