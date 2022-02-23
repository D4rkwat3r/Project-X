package util;

import lib.Console;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class RuCaptcha {

    private final String token;
    private final String siteKey;
    private final String in;
    private final String result;
    private final OkHttpClient okHTTP;

    public RuCaptcha(String token, String siteKey) {
        this.token = token;
        this.siteKey = siteKey;
        this.in = "http://rucaptcha.com/in.php";
        this.result = "http://rucaptcha.com/res.php";
        this.okHTTP = new OkHttpClient();
    }

    private String getB64EncodedContent(String url) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        byte[] content = Objects.requireNonNull(okHTTP.newCall(request).execute().body()).bytes();
        return Base64.getEncoder().encodeToString(content);
    }

    private String getCaptchaSolve(String captchaId) throws IOException, InterruptedException, IllegalStateException {
        TimeUnit.SECONDS.sleep(3);
        StringBuilder url = new StringBuilder();
        url.append(result);
        url.append("?key=");
        url.append(token);
        url.append("&action=");
        url.append("get");
        url.append("&id=");
        url.append(captchaId);
        Request request = new Request.Builder()
                .get()
                .url(url.toString())
                .build();
        String responseString = Objects.requireNonNull(okHTTP.newCall(request).execute().body()).string();
        String[] content = responseString.split(Pattern.quote("|"));
        if (content.length < 2) {
            if (content[0].equals("ERROR_ZERO_BALANCE")) {
                throw new IllegalStateException("ERROR_ZERO_BALANCE");
            }
            return getCaptchaSolve(captchaId);
        }
        return content
                [1];
    }

    public String solveImageCaptcha(String captchaUrl) throws IOException, InterruptedException, IllegalStateException  {
        RequestBody form = new FormBody.Builder()
                .add("method", "base64")
                .add("key", token)
                .add("body", getB64EncodedContent(captchaUrl))
                .build();
        Request request = new Request.Builder()
                .post(form)
                .url(in)
                .build();
        String[] content = Objects.requireNonNull(okHTTP.newCall(request).execute().body())
                .string().
                split(Pattern.quote("|"));
        if (content.length < 2) {
            if (content[0].equals("ERROR_ZERO_BALANCE")) {
                throw new IllegalStateException("ERROR_ZERO_BALANCE");
            }
            return solveImageCaptcha(captchaUrl);
        };
        String captchaId =
                content
                [1];
        return getCaptchaSolve(captchaId);
    }

    public String solveReCaptcha(String pageWithCaptcha) throws IOException, InterruptedException {
        StringBuilder url = new StringBuilder();
        url.append(in);
        url.append("?key=");
        url.append(token);
        url.append("&method=");
        url.append("userrecaptcha");
        url.append("&googlekey=");
        url.append(siteKey);
        url.append("&pageurl=");
        url.append(pageWithCaptcha);
        Request request = new Request.Builder()
                .get()
                .url(url.toString())
                .build();
        String captchaId = Objects.requireNonNull(okHTTP.newCall(request).execute().body())
                .string()
                .split(Pattern.quote("|"))
                [1];
        return getCaptchaSolve(captchaId);
    }

}
