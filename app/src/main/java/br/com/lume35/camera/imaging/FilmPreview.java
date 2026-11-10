package br.com.lume35.camera.imaging;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import java.util.EnumMap;

/** Hardware implementation and cached fallback overlays for API 29–32. */
public final class FilmPreview {
    private static final Bitmap NOISE = makeNoise();
    private final Paint color = new Paint();
    private final Paint[] overlays = {new Paint(3), new Paint(3), new Paint(3), new Paint(3)};
    private boolean fallback;
    private boolean shaderFailed;
    private Object renderer;

    public void install(View view, FilmPipeline plan, int width, int height, boolean original) {
        if (width <= 0 || height <= 0) return;
        fallback = false;
        if (Build.VERSION.SDK_INT >= 33) {
            if (original || plan.strength == 0) {
                view.setRenderEffect(null);
                view.setLayerType(View.LAYER_TYPE_NONE, null);
                return;
            }
            if (!shaderFailed) {
                try {
                    if (renderer == null) renderer = new ShaderRenderer();
                    ((ShaderRenderer) renderer).install(view, plan, width, height);
                    return;
                } catch (IllegalArgumentException | IllegalStateException error) {
                    android.util.Log.w("Lume35", "Film shader unavailable", error);
                    shaderFailed = true;
                }
            }
            view.setRenderEffect(null);
        }
        if (original || plan.strength == 0) {
            view.setLayerType(View.LAYER_TYPE_NONE, null);
            return;
        }
        fallback = true;
        FilmRecipe f = plan.film;
        float[] weights = {f.redMix, f.greenMix, 1 - f.redMix - f.greenMix};
        float[] matrix = new float[20];
        for (int row = 0; row < 3; row++) {
            float slope = (f.curve[row][205] - f.curve[row][43]) * 255 / 162;
            slope += (plan.contrast - 1) * .7f;
            float tone = (f.shadow + f.light) * .5f + plan.warmth;
            float sign = row == 0 ? 1 : row == 1 ? .21f : -1;
            for (int column = 0; column < 3; column++) {
                float mixed = (1 - f.saturation) * weights[column];
                if (column == row) mixed += f.saturation * (1 - f.cross);
                if (column == (row + 1) % 3) mixed += f.saturation * f.cross;
                matrix[row * 5 + column] = (column == row ? 1 - plan.strength : 0)
                        + slope * mixed * plan.strength;
            }
            float ceiling = f == FilmRecipe.EXPIRED || f == FilmRecipe.PORCELANA ? .105f : 10 / 255f;
            float offset = Math.max(-.045f, Math.min(ceiling,
                    f.curve[row][128] - slope * (128 / 255f) + sign * tone + plan.toe));
            matrix[row * 5 + 4] = offset * 255 * plan.strength;
        }
        matrix[18] = 1;
        color.setColorFilter(new ColorMatrixColorFilter(matrix));
        view.setLayerType(View.LAYER_TYPE_HARDWARE, color);
        view.setLayerPaint(color);

        for (Paint p : overlays) p.setShader(null);
        if (plan.vignette > 0) {
            RadialGradient vignette = new RadialGradient(.5f, .5f, .61f, new int[]{
                    0, 0, Color.argb(Math.round(255 * plan.vignette * plan.strength), 0, 0, 0)
            }, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP);
            Matrix transform = new Matrix();
            transform.setScale(width / plan.oval, height);
            transform.postTranslate(width * (1 - 1 / plan.oval) / 2, 0);
            vignette.setLocalMatrix(transform);
            overlays[0].setShader(vignette);
        }
        if (f.grain > 0) {
            BitmapShader noise = new BitmapShader(NOISE, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            Matrix matrixNoise = new Matrix();
            float scale = FilmPipeline.grainScale(width, height);
            matrixNoise.setScale(scale, scale);
            matrixNoise.postTranslate(-plan.offsetX * scale, -plan.offsetY * scale);
            noise.setLocalMatrix(matrixNoise);
            overlays[1].setShader(noise);
            overlays[1].setBlendMode(BlendMode.SOFT_LIGHT);
            overlays[1].setAlpha(Math.round(Math.min(1, 3 * f.grain * plan.strength) * 255));
        }
        for (int i = 0; i < 2; i++) {
            float[] leak = plan.leaks[i];
            if (leak[3] == 0) continue;
            RadialGradient stain = new RadialGradient(leak[0], leak[1], leak[2] * 1.92f,
                    new int[]{Color.argb(Math.round(255 * leak[3] * plan.strength), Math.round(255 * plan.leakR),
                            Math.round(255 * plan.leakG * (i == 0 ? 1 : .487f)),
                            Math.round(255 * plan.leakB * (i == 0 ? 1 : .429f))), 0},
                    null, Shader.TileMode.CLAMP);
            Matrix scale = new Matrix();
            scale.setScale(width, height);
            stain.setLocalMatrix(scale);
            overlays[i + 2].setBlendMode(BlendMode.SCREEN);
            overlays[i + 2].setShader(stain);
        }
    }

    public void draw(Canvas canvas, int width, int height) {
        if (!fallback) return;
        for (Paint p : overlays) if (p.getShader() != null) canvas.drawRect(0, 0, width, height, p);
    }

    private static Bitmap makeNoise() {
        int[] pixels = FilmPipeline.GRAIN.clone();
        for (int i = 0; i < pixels.length; i++) pixels[i] = 0xff000000 | pixels[i] * 0x010101;
        return Bitmap.createBitmap(pixels, FilmPipeline.TILE_SIZE, FilmPipeline.TILE_SIZE, Bitmap.Config.ARGB_8888);
    }

    @TargetApi(33)
    private static final class ShaderRenderer {
        private static final String SOURCE = """
                uniform float amount;
                uniform shader camera;
                uniform shader lut;
                uniform shader grain;
                uniform float2 size;
                uniform float4 tone;
                uniform float4 optical;
                uniform float4 grainParams;
                uniform float4 profile;
                uniform float3 finishValues;
                uniform float4 leakA;
                uniform float4 leakB;
                uniform float3 leakColor;
                float peak(float3 c) { return smoothstep(.73, .99, dot(c, float3(.2126,.7152,.0722))); }
                float stain(float2 uv, float4 l) {
                    float2 d = (uv - l.xy) / max(.01, l.z);
                    return l.w * (1.0 - smoothstep(0.0,3.7,dot(d,d)));
                }
                float3 soft(float3 c, float n) {
                    c = clamp(c,0.0,1.0);
                    float3 d = mix(((16.0*c-12.0)*c+4.0)*c, sqrt(c), step(float3(.25),c));
                    return c + n * (n >= 0.0 ? d-c : c*(1.0-c));
                }
                half4 main(float2 p) {
                    half4 pixel = camera.eval(p);
                    float3 orig = float3(pixel.rgb);
                    float2 uv = p/size;
                    float2 xy = (uv-.5)*2.0*float2(optical.y,1.0);
                    float rad = dot(xy,xy);
                    float edge = smoothstep(.25,1.25,rad);
                    float3 c = orig;
                    if (optical.z > 0.0 || optical.w > 0.0) {
                        float d = max(1.0, min(size.x,size.y)*.0024);
                        float3 l = camera.eval(p-float2(d,0)).rgb;
                        float3 r = camera.eval(p+float2(d,0)).rgb;
                        float3 t = camera.eval(p-float2(0,d)).rgb;
                        float3 b = camera.eval(p+float2(0,d)).rgb;
                        c = mix(c,(l+r+t+b)*.25,optical.z*(.22+.78*edge));
                        c.r = mix(c.r,camera.eval(p+float2(1,0)).r,optical.w*edge);
                        c.b = mix(c.b,camera.eval(p-float2(1,0)).b,optical.w*edge);
                    }
                    float y = dot(c,float3(profile.x,profile.y,1.0-profile.x-profile.y));
                    float sat = tone.x * (1.0-profile.w*(.34*smoothstep(.67,1.0,y)+.32*(1.0-smoothstep(0.0,.24,y))));
                    c = clamp(mix(float3(y),mix(c,c.gbr,profile.z),sat),0.0,1.0)*255.0+.5;
                    float3 mapped = float3(lut.eval(float2(c.r,.5)).r,lut.eval(float2(c.g,.5)).g,lut.eval(float2(c.b,.5)).b);
                    float mid = 4.0*y*(1.0-y);
                    float split = mid*mix(tone.y,tone.z,y);
                    mapped += float3(1,.21,-1)*split + tone.w*(y-.5)*mid + finishValues.x*(1.0-smoothstep(0.0,.34,y));
                    float noise = (grain.eval((p-.5)/grainParams.z+grainParams.xy+.5).r-.5)*5.4*grainParams.w*mid;
                    mapped = soft(mapped,noise)*(1.0-optical.x*smoothstep(.37,1.47,rad));
                    float a = stain(uv,leakA), b = stain(uv,leakB);
                    float halo = 0.0;
                    if (finishValues.y + finishValues.z > 0.0) {
                        float d = min(size.x,size.y)*.016;
                        halo = (2.0*peak(orig)+peak(camera.eval(p+float2(d,0)).rgb)
                            +peak(camera.eval(p-float2(d,0)).rgb)+peak(camera.eval(p+float2(0,d)).rgb)
                            +peak(camera.eval(p-float2(0,d)).rgb))/6.0;
                    }
                    mapped = 1.0-(1.0-mapped)*(1.0-clamp(a*leakColor+b*leakColor*float3(1,.487,.429)+halo*(finishValues.y*float3(1,.24,.07)+finishValues.z*float3(1,.74,.5)),0.0,1.0));
                    return half4(half3(clamp(mix(orig,mapped,amount),0.0,1.0)),pixel.a);
                }
                """;

        private final EnumMap<FilmRecipe, BitmapShader> curves = new EnumMap<>(FilmRecipe.class);
        private final BitmapShader noise = buffer(NOISE, Shader.TileMode.REPEAT);

        void install(View view, FilmPipeline p, int width, int height) {
            RuntimeShader shader = new RuntimeShader(SOURCE);
            shader.setInputBuffer("lut", curves.computeIfAbsent(p.film, ShaderRenderer::curve));
            shader.setInputBuffer("grain", noise);
            shader.setFloatUniform("size", width, height);
            shader.setFloatUniform("amount", p.strength);
            shader.setFloatUniform("tone", p.film.saturation, p.film.shadow + p.warmth,
                    p.film.light + p.warmth, p.contrast - 1);
            shader.setFloatUniform("optical", p.vignette, p.oval, p.softness, p.aberration);
            shader.setFloatUniform("grainParams", p.offsetX, p.offsetY, FilmPipeline.grainScale(width, height), p.film.grain);
            shader.setFloatUniform("profile", p.film.redMix, p.film.greenMix, p.film.cross,
                    (p.film == FilmRecipe.ORIGINAL ? 0 : 1));
            shader.setFloatUniform("finishValues", p.toe, p.halo, p.flare);
            shader.setFloatUniform("leakA", p.leaks[0]);
            shader.setFloatUniform("leakB", p.leaks[1]);
            shader.setFloatUniform("leakColor", p.leakR, p.leakG, p.leakB);
            view.setLayerType(View.LAYER_TYPE_NONE, null);
            view.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "camera"));
        }

        private static BitmapShader curve(FilmRecipe recipe) {
            int[] entries = new int[256];
            for (int i = 0; i < entries.length; i++) entries[i] = Color.rgb(recipe.curve[0][i], recipe.curve[1][i], recipe.curve[2][i]);
            return buffer(Bitmap.createBitmap(entries, 256, 1, Bitmap.Config.ARGB_8888), Shader.TileMode.CLAMP);
        }

        private static BitmapShader buffer(Bitmap image, Shader.TileMode mode) {
            BitmapShader shader = new BitmapShader(image, mode, mode);
            shader.setFilterMode(BitmapShader.FILTER_MODE_LINEAR);
            return shader;
        }
    }
}
