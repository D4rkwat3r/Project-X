package lib;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.*;

public final class HttpUtils {

    static private HttpUtils instance;
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final String[] signables = {
            "rawDeviceId",
            "rawDeviceIdTwo",
            "appType",
            "appVersion",
            "osType",
            "deviceType",
            "sId",
            "countryCode",
            "reqTime",
            "User-Agent",
            "contentRegion",
            "nonce",
            "carrierCountryCodes"
    };


    public String deviceId() {
        String device = null;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
            MessageDigest msgDigest = MessageDigest.getInstance("SHA-1");
            String installationId = UUID.randomUUID().toString();
            stream.write(decoder.decode("Ag=="));
            stream.write(msgDigest.digest(installationId.getBytes(StandardCharsets.UTF_8)));
            byte[] prefix = stream.toByteArray();
            stream.reset();
            stream2.write(prefix);
            stream2.write(msgDigest.digest(decoder.decode("xIgzqEh8x0nmbrk00Lp/LWCK")));
            stream.write(prefix);
            stream.write(msgDigest.digest(stream2.toByteArray()));
            byte[] deviceBytes = stream.toByteArray();
            StringBuilder deviceString = new StringBuilder();
            for (byte eachByte : deviceBytes) {
                deviceString.append(String.format("%02x", eachByte));
            }
            device = deviceString.toString();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return device;
    }

    public String reqSig(Request request, Headers headers) {
        String signature = null;
        String path = relativePath(request.url().toString());
        Map<String, List<String>> multiHeaders = headers.toMultimap();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    decoder.decode("BwXdBGhu8TySKFSThuuRZEZ/6ZsoQHi4mrlstLpsx0g="),
                    "HmacSHA256"
            );
            mac.init(keySpec);
            mac.update(path.getBytes(StandardCharsets.UTF_8));
            for (String signable : signables) {
                List<String> header = multiHeaders.get(signable);
                if (header != null) {
                    for (String part : header) {
                        mac.update(part.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            if (request.body() != null) {
                Buffer buffer = new Buffer();
                Objects.requireNonNull(request.body()).writeTo(buffer);
                mac.update(buffer.readByteArray());
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(decoder.decode("Ag=="));
            stream.write(mac.doFinal());
            signature = encoder.encodeToString(stream.toByteArray());
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return signature;
    }

    public String relativePath(String fullPath) {
        StringBuilder path = new StringBuilder();
        String[] segments = fullPath.split("/");
        path.append("/");
        for (int i = 3; i < segments.length; i++) {
            path.append(segments[i]);
            if (i != segments.length - 1) {
                path.append("/");
            }
        }
        return path.toString();
    }

    public Request rewrite(Request request, Map<String, String> extraHeaders) {
        Headers.Builder headersBuilder = new Headers.Builder()
                .add("appType", "MainApp")
                .add("appVersion", "1.23.4")
                .add("osType", "2")
                .add("deviceType", "1")
                .add("Accept-Language", "en-US")
                .add("countryCode", "EN")
                .add("User-Agent", "com.projz.z.android/1.23.4-12525 (Linux; U; Android 7.1.2; SM-N975F; Build/samsung-user 7.1.2 2)")
                .add("timeZone", "480")
                .add("carrierCountryCodes", "en")
                .add("contentRegion", "100")
                .add("flavor", "google");
        for (String headerKey : request.headers().names()) {
            headersBuilder.add(headerKey, Objects.requireNonNull(request.headers().get(headerKey)));
        }
        for (String headerKey : extraHeaders.keySet()) {
            String header = extraHeaders.get(headerKey);
            headersBuilder.add(headerKey, header);
        }
        if (request.headers().get("Content-Type") == null) {
            headersBuilder.add("Content-Type", "application/json; charset=UTF-8");
        }
        headersBuilder.add("reqTime", String.valueOf(System.currentTimeMillis()));
        headersBuilder.add("nonce", UUID.randomUUID().toString());
        Headers headers = headersBuilder.build();
        return request.newBuilder()
                .method(request.method(), request.body())
                .headers(headers)
                .addHeader("HJTRFS", HttpUtils.getInstance().reqSig(request, headers))
                .build();
    }

    public static HttpUtils getInstance() {
        if (instance == null) {
            instance = new HttpUtils();
        }
        return instance;
    }
}
