package ru.hollowhorizon.hollowengine.client.models.internal.rendering

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33
import ru.hollowhorizon.hollowengine.client.models.internal.*
import ru.hollowhorizon.hollowengine.client.models.internal.utils.VboWrapper
import ru.hollowhorizon.hollowengine.client.models.internal.utils.toFloatBuffer
import ru.hollowhorizon.hollowengine.client.utils.areShadersEnabled
import ru.hollowhorizon.hollowengine.client.utils.toTexture
import java.nio.FloatBuffer

class InstancedRenderer(private val primitive: Primitive) : MeshRenderer {
    private var vao = -1
    private var posBuffer: VboWrapper? = null
    private var norBuffer: VboWrapper? = null
    private var tanBuffer: VboWrapper? = null
    private var uvBuffer: VboWrapper? = null
    private var indexBuffer: VboWrapper? = null
    private var instanceBuffer: VboWrapper? = null

    private var deformer: GpuDeformer? = null
    private val isDynamic = primitive.hasSkinning || primitive.morphTargets.isNotEmpty()

    private var instanceDataBuffer: FloatBuffer? = null

    override fun init() {
        vao = GL33.glGenVertexArrays()
        GL33.glBindVertexArray(vao)

        // Инициализируем статические буферы геометрии
        if (isDynamic) {
            val vertexCount = primitive.positionsCount / 3
            posBuffer = VboWrapper.createArrayBuffer().apply {
                allocate(vertexCount * 3 * 4L, GL33.GL_DYNAMIC_COPY)
                GL33.glVertexAttribPointer(0, 3, GL33.GL_FLOAT, false, 0, 0)
                GL33.glEnableVertexAttribArray(0)
            }
            norBuffer = VboWrapper.createArrayBuffer().apply {
                allocate(vertexCount * 3 * 4L, GL33.GL_DYNAMIC_COPY)
                GL33.glVertexAttribPointer(5, 3, GL33.GL_FLOAT, false, 0, 0)
                GL33.glEnableVertexAttribArray(5)
            }
            tanBuffer = VboWrapper.createArrayBuffer().apply {
                allocate(vertexCount * 4 * 4L, GL33.GL_DYNAMIC_COPY)
                GL33.glVertexAttribPointer(9, 4, GL33.GL_FLOAT, false, 0, 0)
                GL33.glEnableVertexAttribArray(9)
            }

            deformer = GpuDeformer(primitive)
            deformer?.init(posBuffer!!.id, norBuffer!!.id, tanBuffer!!.id)
        } else {
            primitive.positions?.let { positions ->
                posBuffer = VboWrapper.createArrayBuffer().apply {
                    val data = positions.toFloatBuffer(3) { v, b -> b.put(v.x).put(v.y).put(v.z) }
                    uploadData(data)
                    GL33.glVertexAttribPointer(0, 3, GL33.GL_FLOAT, false, 0, 0)
                    GL33.glEnableVertexAttribArray(0)
                }
            }

            primitive.normals?.let { normals ->
                norBuffer = VboWrapper.createArrayBuffer().apply {
                    val data = normals.toFloatBuffer(3) { v, b -> b.put(v.x).put(v.y).put(v.z) }
                    uploadData(data)
                    GL33.glVertexAttribPointer(5, 3, GL33.GL_FLOAT, false, 0, 0)
                    GL33.glEnableVertexAttribArray(5)
                }
            }

            primitive.tangents?.let { tangents ->
                tanBuffer = VboWrapper.createArrayBuffer().apply {
                    val data = tangents.toFloatBuffer(4) { v, b -> b.put(v.x).put(v.y).put(v.z).put(v.w) }
                    uploadData(data)
                    GL33.glVertexAttribPointer(9, 4, GL33.GL_FLOAT, false, 0, 0)
                    GL33.glEnableVertexAttribArray(9)
                }
            }
        }

        primitive.texCoords?.let { uvs ->
            uvBuffer = VboWrapper.createArrayBuffer().apply {
                val data = uvs.toFloatBuffer(2) { v, b -> b.put(v.x).put(v.y) }
                uploadData(data)
                GL33.glVertexAttribPointer(2, 2, GL33.GL_FLOAT, false, 0, 0)
                GL33.glEnableVertexAttribArray(2)
            }
        }

        primitive.indices?.let { indices ->
            indexBuffer = VboWrapper.createElementBuffer().apply {
                val buffer = BufferUtils.createIntBuffer(indices.size)
                buffer.put(indices)
                buffer.flip()
                uploadData(buffer)
            }
        }

        // Instance Buffer для матриц (атрибуты 10-13)
        instanceBuffer = VboWrapper.createArrayBuffer()
        instanceBuffer?.bind()
        for (i in 0 until 4) {
            val index = 6 + i
            GL33.glEnableVertexAttribArray(index)
            GL33.glVertexAttribPointer(index, 4, GL33.GL_FLOAT, false, 64, (i * 16).toLong())
            GL33.glVertexAttribDivisor(index, 1)
        }

        GL33.glBindVertexArray(0)
    }

    override fun setupPipeline(
        pipeline: RenderPipeline,
        skinGetter: SkinGetter,
        matrixGetter: MatrixGetter,
        visibilityGetter: VisibilityGetter
    ) {
        if (!visibilityGetter()) return
        if (pipeline is InstancedRenderPipeline) {
            pipeline.onUpdate {
                if (isDynamic && deformer != null) {
                    deformer!!.compute(skinGetter)
                }
                pipeline.addInstance(primitive, matrixGetter(), this)
            }
        }
    }

    fun render(stack: Matrix4f, count: Int, matrices: FloatBuffer) {
        val shader = RenderSystem.getShader() ?: return
        applyMaterial(shader, primitive.material)

        instanceBuffer?.bind()
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, matrices, GL33.GL_STREAM_DRAW)

        GL33.glBindVertexArray(vao)
        indexBuffer?.bind()

        val modelView = Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack)
        shader.MODEL_VIEW_MATRIX?.set(modelView)
        shader.MODEL_VIEW_MATRIX?.upload()

        val indexCount = primitive.indices?.size ?: (primitive.positionsCount / 3)
        GL33.glDrawElementsInstanced(GL33.GL_TRIANGLES, indexCount, GL33.GL_UNSIGNED_INT, 0L, count)

        GL33.glBindVertexArray(0)
    }

    private fun applyMaterial(shader: ShaderInstance, material: Material) {
        GL33.glVertexAttrib4f(1, material.color.r, material.color.g, material.color.b, material.color.a)

        if (areShadersEnabled) {
            GL33.glGetUniformLocation(shader.id, "normals").takeIf { it != -1 }?.let {
                RenderSystem.activeTexture(COLOR_MAP_INDEX + GL33.glGetUniformi(shader.id, it))
                RenderSystem.bindTexture(material.normalTexture.toTexture().id)
            }
            GL33.glGetUniformLocation(shader.id, "specular").takeIf { it != -1 }?.let {
                RenderSystem.activeTexture(COLOR_MAP_INDEX + GL33.glGetUniformi(shader.id, it))
                RenderSystem.bindTexture(material.specularTexture.toTexture().id)
            }
        }

        RenderSystem.activeTexture(COLOR_MAP_INDEX)
        RenderSystem.bindTexture(Minecraft.getInstance().textureManager.getTexture(material.texture).id)

        if (material.doubleSided) RenderSystem.disableCull()
        else RenderSystem.enableCull()

        when (material.blend) {
            Material.Blend.OPAQUE -> RenderSystem.disableBlend()
            Material.Blend.BLEND -> {
                RenderSystem.enableBlend()
                RenderSystem.defaultBlendFunc()
            }
        }
    }

    override fun destroy() {
        GL33.glDeleteVertexArrays(vao)
        posBuffer?.delete()
        norBuffer?.delete()
        tanBuffer?.delete()
        uvBuffer?.delete()
        indexBuffer?.delete()
        instanceBuffer?.delete()
        deformer?.destroy()
    }
}
