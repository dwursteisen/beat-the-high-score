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

vec4 effect(vec2 uv)
{

    float amplitude =  0.8 * (1. - alpha);
    float offset = cos((uv.y + alpha) * 12.);
    float x = (uv.x + offset * amplitude);

    return texture2D(u_texture, vec2(x, uv.y));
}


void main() {
        // Output to screen
        gl_FragColor = effect(v_texCoords);

}
