package util;

public class Config {
    private final String ruCaptchaKey;
    private final String reCaptchaToken;

    public Config(String ruCaptchaKey, String reCaptchaToken) {
        this.ruCaptchaKey = ruCaptchaKey;
        this.reCaptchaToken = reCaptchaToken;
    }

    public String getRuCaptchaKey() {
        return ruCaptchaKey;
    }

    public String getReCaptchaToken() {
        return reCaptchaToken;
    }
}
