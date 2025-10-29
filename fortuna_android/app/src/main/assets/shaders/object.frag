#version 300 es
/*
 * Fragment shader for rendering 3D objects in AR
 * Supports both textured and solid color rendering
 */
precision mediump float;

uniform sampler2D u_Texture;   // Texture sampler
uniform vec4 u_Color;           // Solid color (fallback)
uniform int u_UseTexture;       // 1 = use texture, 0 = use color
uniform vec3 u_LightDirection;
uniform float u_LightIntensity;

in vec3 v_ViewPosition;
in vec3 v_ViewNormal;
in vec2 v_TexCoord;

out vec4 o_FragColor;

void main() {
    // Choose base color: texture or solid color
    vec4 baseColor;
    if (u_UseTexture == 1) {
        baseColor = texture(u_Texture, v_TexCoord);
    } else {
        baseColor = u_Color;
    }

    // Simple diffuse lighting
    vec3 normal = normalize(v_ViewNormal);
    vec3 lightDir = normalize(u_LightDirection);
    float diffuse = max(dot(normal, lightDir), 0.0);

    // Add ambient light
    float ambient = 0.4;
    float lighting = ambient + diffuse * u_LightIntensity;

    // Apply lighting to base color
    vec3 litColor = baseColor.rgb * lighting;

    o_FragColor = vec4(litColor, baseColor.a);
}