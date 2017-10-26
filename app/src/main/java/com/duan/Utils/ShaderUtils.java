package com.duan.Utils;

/**
 * Created by duanyy on 2017/10/25.
 */

public class ShaderUtils {

    public static final String VERTEX_SHADER_SIMPLE = "uniform mat4 u_MVPMatrix; " +
            "attribute vec4 a_position;" +
            "attribute vec2 a_textCoord;"+
            "varying vec2 v_textCoord;"+
            "void main()" +
            "{" +
            "    gl_Position = u_MVPMatrix * a_position;" +
//            "    gl_Position = u_MVPMatrix * vec4(a_position.x,a_position.y,a_position.z,1.0);" +
            "    v_textCoord = a_textCoord;"+
            "}";

    public static final String FRAGMENT_SHADER_SIMPLE = "precision mediump float;" +
            "varying vec2 v_textCoord;"+
            "uniform sampler2D u_sampleTexture;"+
            "void main(){" +
            "  gl_FragColor = texture2D(u_sampleTexture,v_textCoord);" +
            "}";

    public static final String FRAGMENT_SHADER_OES =
            "#extension GL_OES_EGL_image_external : require\n " +
                    "precision mediump float;" +
                    "varying vec2 v_textCoord;" +
                    "uniform samplerExternalOES u_sampleTexture;" +
                    "void main()" +
                    "{" +
                    "   gl_FragColor = texture2D(u_sampleTexture,v_textCoord);" +
                    "}";


//    "precision mediump float;" +
//            "varying vec2 textureCoordinate;"+
//            "uniform sampler2D u_sampleTexture;"+
//            "void main(){" +
//            "  gl_FragColor = texture2D(u_sampleTexture,textureCoordinate);" +
//            "}";
}
