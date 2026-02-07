package ru.hollowhorizon.hollowengine.client.models.internal.rendering

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import de.fabmax.kool.math.Mat4f
import net.minecraft.client.Minecraft
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33
import ru.hollowhorizon.hollowengine.client.models.internal.Primitive
import ru.hollowhorizon.hollowengine.client.models.internal.drawWithShader
import ru.hollowhorizon.hollowengine.client.models.internal.manager.HollowModelManager
import ru.hollowhorizon.hollowengine.common.registry.ModShaders
import java.nio.FloatBuffer

class InstancedRenderPipeline : RenderPipeline {
    private val instances = mutableMapOf<Primitive, MutableList<Matrix4f>>()
    private val renderers = mutableMapOf<Primitive, InstancedRenderer>()
    private var buffer: FloatBuffer = BufferUtils.createFloatBuffer(16 * 1024)

    private val commands = ArrayList<() -> Unit>()
    private val skinCommands = ArrayList<() -> Unit>()

    override fun addBatchedRenderable(action: Renderable) {
    }

    override fun addVAORenderable(action: Renderable) {
    }

    override fun onUpdate(action: () -> Unit) {
        commands.add(action)
    }

    override fun addSkinnable(action: () -> Unit) {
    }

    fun addInstance(primitive: Primitive, matrix: Mat4f, renderer: InstancedRenderer) {
        val jomlMatrix = Matrix4f(
            matrix.m00, matrix.m01, matrix.m02, matrix.m03,
            matrix.m10, matrix.m11, matrix.m12, matrix.m13,
            matrix.m20, matrix.m21, matrix.m22, matrix.m23,
            matrix.m30, matrix.m31, matrix.m32, matrix.m33
        ).transpose() // Kool Mat4f is row-major, JOML is column-major
        instances.getOrPut(primitive) { mutableListOf() }.add(jomlMatrix)
        renderers[primitive] = renderer
    }

    override fun render(context: RenderContext) {
        for (action in commands.parallelStream()) action()

        if (instances.isEmpty()) return

        val activeTexture = GlStateManager._getActiveTexture()
        val currentVAO = GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING)

        // Setup common uniforms/textures (similar to RenderPipeline.renderVAO)
        // В ванильном шейдере UV1 и UV2 используются для оверлея и света
        GL33.glVertexAttribI2i(3, context.overlay and 0xFFFF, context.overlay shr 16 and 0xFFFF)
        GL33.glVertexAttribI2i(4, context.light and 0xFFFF, context.light shr 16 and 0xFFFF)

        RenderSystem.activeTexture(GL33.GL_TEXTURE2)
        val texture2 = GlStateManager.TEXTURES[GlStateManager.activeTexture].binding
        RenderSystem.bindTexture(HollowModelManager.lightTexture.id)

        RenderSystem.activeTexture(GL33.GL_TEXTURE1)
        val texture1 = GlStateManager.TEXTURES[GlStateManager.activeTexture].binding
        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor()
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(1))
        Minecraft.getInstance().gameRenderer.overlayTexture().teardownOverlayColor()

        RenderSystem.activeTexture(GL33.GL_TEXTURE0)
        val texture0 = GlStateManager.TEXTURES[GlStateManager.activeTexture].binding

        RenderSystem.setShader { ModShaders.GLTF_INSTANCED }

        drawWithShader {
            for ((primitive, matrices) in instances) {
                val renderer = renderers[primitive] ?: continue

                if (buffer.capacity() < matrices.size * 16) {
                    buffer = BufferUtils.createFloatBuffer(matrices.size * 16)
                }
                buffer.clear()
                for (m in matrices) {
                    m.get(buffer)
                    buffer.position(buffer.position() + 16)
                }
                buffer.flip()

                renderer.render(context.stack.last().pose(), matrices.size, buffer)
            }
        }

        RenderSystem.activeTexture(GL33.GL_TEXTURE2)
        RenderSystem.bindTexture(texture2)
        RenderSystem.activeTexture(GL33.GL_TEXTURE1)
        RenderSystem.bindTexture(texture1)
        RenderSystem.activeTexture(GL33.GL_TEXTURE0)
        RenderSystem.bindTexture(texture0)
        RenderSystem.activeTexture(activeTexture)
        RenderSystem.glBindVertexArray { currentVAO }

        instances.clear()
    }

    override fun clear() {
        instances.clear()
    }
}
