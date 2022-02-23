package lib;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.*;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class HttpClient extends OkHttpClient {

    Map<String, String> extraHeaders;

    public HttpClient() {
        this.extraHeaders = new HashMap<>();
    }


    public void addExtraHeader(String key, String value) {
        extraHeaders.put(key, value);
    }

    public void replaceExtraHeader(String key, String value) {
        extraHeaders.remove(key);
        extraHeaders.put(key, value);
    }

    @NotNull
    @Override
    public Call newCall(@NotNull Request request) {
        return super.newCall(HttpUtils.getInstance().rewrite(request, extraHeaders));
    }

    @NotNull
    @Override
    public WebSocket newWebSocket(@NotNull Request request, @NotNull WebSocketListener listener) {
        return super.newWebSocket(HttpUtils.getInstance().rewrite(request, extraHeaders), listener);
    }

    public ResponseWrapper postJson(String url, String body) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, MediaType.get("application/json; charset=utf-8")))
                .build();
        try {
            return ResponseWrapper.wrap(newCall(request).execute());
        } catch (SocketTimeoutException e) {
            return postJson(url, body);
        }
    }

    public ResponseWrapper postEmpty(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .method("POST", RequestBody.create(new byte[0], MediaType.get("application/json; charset=utf-8")))
                .build();
        try {
            return ResponseWrapper.wrap(newCall(request).execute());
        } catch (SocketTimeoutException e) {
            return postEmpty(url);
        }
    }

    public ResponseWrapper delete(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        try {
            return ResponseWrapper.wrap(newCall(request).execute());
        } catch (SocketTimeoutException e) {
            return delete(url);
        }
    }

    public ResponseWrapper get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try {
            return ResponseWrapper.wrap(newCall(request).execute());
        } catch (SocketTimeoutException e) {
            return get(url);
        }
    }

    private File inputStreamToFile(InputStream stream) throws IOException {
        File file = File.createTempFile(stream.toString(), ".tmp");
        file.deleteOnExit();
        FileOutputStream outputStream = new FileOutputStream(file);
        stream.transferTo(outputStream);
        return file;
    }

    private JSONObject uploadMultiPart(String url, File file) throws IOException {
        MultipartBody.Part part = MultipartBody.Part.createFormData("media", file.getName(), RequestBody.create(MediaType.get("image/jpeg"), file));
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(part)
                .build();
        Request request = new Request.Builder()
                .method("POST", body)
                .url(url)
                .addHeader("Content-Type", "multipart/form-data")
                .build();
        try {
            String strBody = Objects.requireNonNull(newCall(request).execute().body()).string();
            return new JSONObject(strBody);
        } catch (SocketTimeoutException e) {
            return uploadMultiPart(url, file);
        }
    }

    public JSONObject postMultipartFile(String url, File file) throws IOException {
        return uploadMultiPart(url, file);
    }

    public JSONObject postMultipartFile(String url, InputStream stream) throws IOException {
        return uploadMultiPart(url, inputStreamToFile(stream));
    }

}
