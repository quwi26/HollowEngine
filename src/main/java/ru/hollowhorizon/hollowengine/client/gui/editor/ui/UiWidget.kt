package ru.hollowhorizon.hollowengine.client.gui.editor.ui

import de.fabmax.kool.math.Easing
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.util.Color
import ru.hollowhorizon.hollowengine.client.gui.scripting.tools.hoverable

class UiWidget(val content: Composable) : Composable {
    val borderMode = mutableStateOf(0)
    val backgroundMode = mutableStateOf(0)

    val backgroundColor = ColorHSV()
    val borderColor = ColorHSV()
    val roundedCorners = mutableStateOf(0f)

    val dockable = UiDockable("UiWidget")
    val offsetX get() = dockable.floatingX
    val offsetY get() = dockable.floatingY
    val width get() = dockable.floatingWidth
    val height get() = dockable.floatingHeight
    var isSelected = mutableStateOf(false)

    override fun UiScope.compose() {
        Window(dockable, borderColor = null, backgroundColor = null) {
            val isHovered by modifier.hoverable()
            val color by animateColorAsState(
                if (isSelected.use()) Color("035afc")
                else Color("c2c2c2").withAlpha(if (isHovered) 1f else 0f),
                tween(0.15f, Easing.easeOutQuart)
            )
            modifier
                .border(RectBorder(color, sizes.borderWidth))
                .onClick {
                    if (it.isLeftClick) isSelected.set(!isSelected.use())
                }
            with(dockable) { registerDragCallbacks() }

            Box {
                when(backgroundMode.use()) {
                    1 -> modifier.backgroundColor(
                        Color.Hsv(
                            backgroundColor.hue.use(),
                            backgroundColor.sat.use(),
                            backgroundColor.value.use(),
                        ).toSrgb(a=backgroundColor.alpha.use())
                    )
                    2 -> {
                        modifier.background(
                            RoundRectBackground(
                                Color.Hsv(
                                    backgroundColor.hue.use(),
                                    backgroundColor.sat.use(),
                                    backgroundColor.value.use(),
                                ).toSrgb(a=backgroundColor.alpha.use()),
                                roundedCorners.use().dp
                            )
                        )
                    }
                }
                when(borderMode.use()) {
                    1 -> modifier.border(
                        RectBorder(
                        Color.Hsv(
                            borderColor.hue.use(),
                            borderColor.sat.use(),
                            borderColor.value.use(),
                        ).toSrgb(a=borderColor.alpha.use()),
                            sizes.borderWidth
                        )
                    )
                    2 -> {
                        modifier.border(
                            RoundRectBorder(
                                Color.Hsv(
                                    borderColor.hue.use(),
                                    borderColor.sat.use(),
                                    borderColor.value.use(),
                                ).toSrgb(a=borderColor.alpha.use()),
                                roundedCorners.use().dp,
                                sizes.borderWidth
                            )
                        )
                    }
                }

                content()
            }
        }
    }
}

class ColorHSV {
    val hue = mutableStateOf(0f)
    val sat = mutableStateOf(1f)
    val value = mutableStateOf(1f)
    val alpha = mutableStateOf(1f)
    val hexString = mutableStateOf("")
}