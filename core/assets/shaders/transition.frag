#ifdef GL_ES
    precision highp float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform mat4 u_projTrans;

uniform float alpha;


void main() {
        vec3 color = texture2D(u_texture, v_texCoords).rgb;

        // cutoff computation
        vec3 cutoff = vec3(1, 1, 1) * alpha;
        if(color.r <= cutoff.r && color.g <= cutoff.g, color.b <= cutoff.b) {
            gl_FragColor = vec4(34.0 / 256., 32. / 256., 52.0 / 256., 1.0);
        } else {
            gl_FragColor = vec4(color, 0.0);
        }

}