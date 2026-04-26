#version 410 core

in vec4 v_Color;
in vec2 v_TexCoord;

uniform sampler2D Sampler0;

layout(std140) uniform TtfInfo {
    float EdgeThreshold;
    float AntiAliasingEnabled;
};

layout(location = 0) out vec4 f_Color;

float coverage(vec2 uv, float aa) {
    float d = 1.0 - texture(Sampler0, uv).r;
    return smoothstep(EdgeThreshold - aa, EdgeThreshold + aa, d);
}

void main() {
    float distance = 1.0 - texture(Sampler0, v_TexCoord).r;
    float alpha;

    if (AntiAliasingEnabled > 0.5) {
        vec2 dx = dFdx(v_TexCoord);
        vec2 dy = dFdy(v_TexCoord);

        float aa = clamp(fwidth(distance) * 0.5, 0.0008, 0.5);

        const vec2 o0 = vec2(0.125, 0.375);
        const vec2 o1 = vec2(0.375, -0.125);
        const vec2 o2 = vec2(-0.125, -0.375);
        const vec2 o3 = vec2(-0.375, 0.125);

        alpha = coverage(v_TexCoord + dx * o0.x + dy * o0.y, aa);
        alpha += coverage(v_TexCoord + dx * o1.x + dy * o1.y, aa);
        alpha += coverage(v_TexCoord + dx * o2.x + dy * o2.y, aa);
        alpha += coverage(v_TexCoord + dx * o3.x + dy * o3.y, aa);
        alpha *= 0.25;
    } else {
        float afwidth = fwidth(distance) * 0.5;

        alpha = smoothstep(EdgeThreshold - afwidth, EdgeThreshold + afwidth, distance);
    }

    f_Color = vec4(v_Color.rgb, v_Color.a * alpha);

    if (f_Color.a < 0.005) {
        discard;
    }
}