package ru.hollowhorizon.hollowengine.client.gui.editor.ui

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import ru.hollowhorizon.hollowengine.client.gui.scripting.popup.ItemPopupMenu

class UiEditor : Composable {
    val widgets = mutableListOf<UiWidget>()
    private var draggedWidget: UiWidget? = null
    private var localDragX = 0f
    private var localDragY = 0f
    private val creationPopup = ItemPopupMenu<Vec2f>("BlockCreationMenu")
    override fun UiScope.compose() {
        Row(Grow.Std, Grow.Std) {
            Box(Grow.Std, Grow.Std) {
                val root = uiNode
                widgets.forEach { widget ->
                    widget()
                }

                modifier.onClick {
                    if (it.isRightClick) {
                        creationPopup.show(
                            Vec2f(it.screenPosition),
                            makePopup(root),
                            Vec2f(it.screenPosition)
                        )
                    }
                }

                creationPopup()
            }
            EditorPanel()
        }
    }

    private fun handleDragStart(block: UiWidget, ev: PointerEvent) {
        draggedWidget = block
        localDragX = ev.position.x
        localDragY = ev.position.y
    }

    private fun handleDrag(block: UiWidget, ev: PointerEvent, root: UiNode) {
        if (draggedWidget == block) {
            val rootDrag = root.toLocal(ev.screenPosition)
            val deltaX = rootDrag.x - localDragX
            val deltaY = rootDrag.y - localDragY

            block.offsetX.set(Dp.fromPx(deltaX))
            block.offsetY.set(Dp.fromPx(deltaY))
        }
    }

    private fun handleDragEnd(block: UiWidget, ev: PointerEvent, root: UiNode) {
        draggedWidget = null
        val rootDrag = root.toLocal(ev.screenPosition)
        val deltaX = rootDrag.x - localDragX
        val deltaY = rootDrag.y - localDragY

        block.offsetX.set(Dp.fromPx(deltaX))
        block.offsetY.set(Dp.fromPx(deltaY))
    }

    fun addWidget(position: Vec2f, action: UiScope.() -> Unit) {
        widgets.add(
            UiWidget(action).apply {
                offsetX.set(Dp.fromPx(position.x))
                offsetY.set(Dp.fromPx(position.y))
            }
        )
    }

    private fun UiScope.EditorPanel() {
        Column(350.dp, Grow.Std) {
            modifier.backgroundColor(Color.BLACK.withAlpha(0.4f))
                .padding(sizes.smallGap)
            Text("Трансформация") { }

            val widget = widgets.firstOrNull { it.isSelected.use() } ?: return@Column

            Position(widget)
            Size(widget)

            Text("Внешний вид") {}

            GeneralStyle(widget)
        }
    }

    private fun UiScope.Position(widget: UiWidget) {
        Row {
            Text("X: ") {}
            TextField(
                widget.offsetX.use().px.toString()
            ) { modifier.onChange { widget.offsetX.set(Dp.fromPx(it.toFloatOrNull() ?: 0f)) } }
            Text(" Y: ") {}
            TextField(
                widget.offsetY.use().px.toString()
            ) { modifier.onChange { widget.offsetY.set(Dp.fromPx(it.toFloatOrNull() ?: 0f)) } }
        }
    }

    private fun UiScope.Size(widget: UiWidget) {
        Row {
            Text("Ширина: ") {}
            TextField(
                (widget.width.use() as? Dp)?.px.toString()
            ) { modifier.onChange { widget.width.set(Dp.fromPx(it.toFloatOrNull() ?: 100f)) } }
            Text(" Высота: ") {}
            TextField(
                (widget.height.use() as? Dp)?.px.toString()
            ) { modifier.onChange { widget.height.set(Dp.fromPx(it.toFloatOrNull() ?: 100f)) } }
        }
    }

    private fun UiScope.GeneralStyle(widget: UiWidget) {
        Row {

            Text("Фон: ") {}

            ComboBox {
                modifier.items(listOf("Нет", "Цвет", "Скругленный"))
                    .selectedIndex(widget.backgroundMode.use())
                    .onItemSelected { widget.backgroundMode.set(it) }
            }
        }

        when (widget.backgroundMode.use()) {
            1 -> {
                Text("Цвет") {}
                ColorChooserH(
                    widget.backgroundColor.hue,
                    widget.backgroundColor.sat,
                    widget.backgroundColor.value,
                    widget.backgroundColor.alpha,
                    widget.backgroundColor.hexString
                )
            }

            2 -> {
                Text("Цвет") {}
                ColorChooserH(
                    widget.backgroundColor.hue,
                    widget.backgroundColor.sat,
                    widget.backgroundColor.value,
                    widget.backgroundColor.alpha,
                    widget.backgroundColor.hexString
                )
                Text("Радиус") {}
                Slider(widget.roundedCorners.use(), 0f, 90f) {
                    modifier.onChange { widget.roundedCorners.set(it) }
                }
            }
        }

        Row {
            Text("Рамка") {}

            ComboBox {
                modifier.items(listOf("Нет", "Цвет", "Скругленная"))
                    .selectedIndex(widget.borderMode.use())
                    .onItemSelected { widget.borderMode.set(it) }
            }
        }

        when (widget.borderMode.use()) {
            1 -> {
                Text("Цвет") {}
                ColorChooserV(
                    widget.borderColor.hue,
                    widget.borderColor.sat,
                    widget.borderColor.value,
                    widget.borderColor.alpha,
                    widget.borderColor.hexString
                )
            }

            2 -> {
                Text("Цвет") {}
                ColorChooserV(
                    widget.borderColor.hue,
                    widget.borderColor.sat,
                    widget.borderColor.value,
                    widget.borderColor.alpha,
                    widget.borderColor.hexString
                )
                Text("Радиус") {}
                Slider(widget.roundedCorners.use(), 0f, 90f) {
                    modifier.onChange { widget.roundedCorners.set(it) }
                }
            }
        }
    }
}