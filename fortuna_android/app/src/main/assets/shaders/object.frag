#version 300 es
/*
 * Fragment shader for rendering 3D objects in AR
 */
precision mediump float;

uniform vec4 u_Color;
uniform vec3 u_LightDirection;
uniform float u_LightIntensity;

in vec3 v_ViewPosition;
in vec3 v_ViewNormal;
in vec2 v_TexCoord;

out vec4 o_FragColor;

void main() {
    // Simple diffuse lighting
    vec3 normal = normalize(v_ViewNormal);
    vec3 lightDir = normalize(u_LightDirection);
    float diffuse = max(dot(normal, lightDir), 0.0);

    // Add ambient light
    float ambient = 0.3;
    float lighting = ambient + diffuse * u_LightIntensity;

    // Apply lighting to color
    vec3 litColor = u_Color.rgb * lighting;

    o_FragColor = vec4(litColor, u_Color.a);
}