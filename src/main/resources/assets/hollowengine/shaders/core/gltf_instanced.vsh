#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

// Instance attributes
in mat4 InstanceMat; // Использует 10, 11, 12, 13 атрибуты

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;

void main() {
    // Применяем матрицу инстанса к позиции
    vec4 worldPos = InstanceMat * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * worldPos;

    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * worldPos.xyz, FogShape);
    
    // Для нормалей используем верхнюю левую часть 3x3 матрицы инстанса
    mat3 instanceNormalMat = mat3(InstanceMat);
    vec3 fixNormal = normalize(instanceNormalMat * Normal);
    
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, fixNormal, Color);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
    normal = vec4(fixNormal, 0.0);
}
