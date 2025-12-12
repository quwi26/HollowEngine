package ru.hollowhorizon.hollowengine.client.gui.editor.ui

import de.fabmax.kool.modules.ui2.Text
import de.fabmax.kool.modules.ui2.UiNode
import ru.hollowhorizon.hollowengine.client.gui.scripting.popup.SubMenuItem
import ru.hollowhorizon.hollowengine.client.kool.minecraft.Image

context(editor: UiEditor)
fun makePopup(root: UiNode) = SubMenuItem("Интерфейс") {
    item("Текст") {
        editor.addWidget(root.toLocal(it)) {
            Text("Какой-то текст") {

            }
        }
    }
    item("Картинка") {
        editor.addWidget(root.toLocal(it)) {
            Image("hollowengine:textures/gui/dialogues/background.png") {  }
        }
    }

}