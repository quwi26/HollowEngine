package ru.hollowhorizon.hollowengine.client.gui.scripting.panels

import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.docking.Dock
import ru.hollowhorizon.hollowengine.client.gui.editor.ui.UiEditor

class UiEditorPanel(dock: Dock): DockPanel("hollowengine.gui.ide.ui", dock) {
    override val icon: String = "hollowengine:textures/gui/icons/recipes.svg"
    val editor = UiEditor()

    override fun UiScope.compose() {
        editor()
    }

}