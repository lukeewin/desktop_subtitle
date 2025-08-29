package top.lukeewin.subtitle.entity;

/**
 * @author Luke Ewin
 * @date 2025/8/22 2:06
 * @blog blog.lukeewin.top
 */
public class ASRConfigEntity {
    private String modelEncoder;
    private String modelDecoder;
    private String modelTokens;

    public ASRConfigEntity() {
    }

    public ASRConfigEntity(String modelEncoder, String modelDecoder, String modelTokens) {
        this.modelEncoder = modelEncoder;
        this.modelDecoder = modelDecoder;
        this.modelTokens = modelTokens;
    }

    public String getModelEncoder() {
        return modelEncoder;
    }

    public void setModelEncoder(String modelEncoder) {
        this.modelEncoder = modelEncoder;
    }

    public String getModelDecoder() {
        return modelDecoder;
    }

    public void setModelDecoder(String modelDecoder) {
        this.modelDecoder = modelDecoder;
    }

    public String getModelTokens() {
        return modelTokens;
    }

    public void setModelTokens(String modelTokens) {
        this.modelTokens = modelTokens;
    }
}
