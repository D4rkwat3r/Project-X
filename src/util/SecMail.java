package util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

public final class SecMail {

    private final String login;
    private final String domain;
    private final String fullAddress;
    private final OkHttpClient okHTTP;
    private final String apiUrl;

    private SecMail(String login, String domain) {
        this.login = login;
        this.domain = domain;
        this.fullAddress = login + "@" + domain;
        this.okHTTP = new OkHttpClient();
        this.apiUrl = "https://www.1secmail.com/api/v1/";
    }

    public int receiveMessage() throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(apiUrl + String.format("?action=getMessages&login=%s&domain=%s", login, domain))
                .build();
        Response response = okHTTP.newCall(request).execute();
        JSONArray messages = new JSONArray(response.body().string());
        if (messages.length() == 0) {
            return receiveMessage();
        }
        return messages.getJSONObject(0).getInt("id");
    }

    public String getUrl() {
        String foundUrl = null;
        try {
            int id = receiveMessage();
            Request request = new Request.Builder()
                    .get()
                    .url(apiUrl + String.format("?action=readMessage&login=%s&domain=%s&id=%d", login, domain, id))
                    .build();
            Response response = okHTTP.newCall(request).execute();
            String html = new JSONObject(response.body().string()).getString("htmlBody");
            foundUrl = html.substring(html.indexOf("http"), html.indexOf(".png")) + ".png";
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return foundUrl;
    }

    public String getFullAddress() {
        return this.fullAddress;
    }

    private static String randomString(int length) {
        String population = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        char[] array = new char[length];
        for (int i = 0; i < length; i++) {
            array[i] = population.charAt(random.nextInt(population.length()));
        }
        return new String(array);
    }

    public static SecMail generate() {
        return new SecMail(
                randomString(10),
                "1secmail.net"
        );
    }
}
