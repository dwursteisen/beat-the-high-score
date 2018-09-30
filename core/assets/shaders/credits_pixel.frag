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
   if(alpha < 0.01) {
     return texture2D(u_texture, uv);
   }
   vec2 pixel = vec2(40., 40.)  * alpha;
   vec2 ratio =  pixel / res;
   vec2 target = (floor(uv / ratio) + 0.5) * ratio;
   return texture2D(u_texture, target);
}


void main() {
        // Output to screen
        gl_FragColor = effect(v_texCoords);

}