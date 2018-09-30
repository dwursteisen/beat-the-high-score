#ifdef GL_ES
    precision highp float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_reference;
uniform mat4 u_projTrans;
uniform float alpha;
uniform vec2 res;

void main() {
    // Output to screen
    gl_FragColor = texture2D(u_texture, v_texCoords);

}