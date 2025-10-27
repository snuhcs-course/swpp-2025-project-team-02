#version 300 es
/*
 * Vertex shader for rendering 3D objects in AR
 */

uniform mat4 u_ModelViewProjection;
uniform mat4 u_ModelView;

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoord;
layout(location = 2) in vec3 a_Normal;

out vec3 v_ViewPosition;
out vec3 v_ViewNormal;
out vec2 v_TexCoord;

void main() {
    v_ViewPosition = (u_ModelView * vec4(a_Position, 1.0)).xyz;
    v_ViewNormal = normalize((u_ModelView * vec4(a_Normal, 0.0)).xyz);
    v_TexCoord = a_TexCoord;
    gl_Position = u_ModelViewProjection * vec4(a_Position, 1.0);
}