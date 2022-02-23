package util;

import lib.HttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;

public class ProjZWebSocket extends WebSocketListener {

    private final Timer pinger;
    private WebSocket parent;

    public void startPingResolver() {
        this.pinger.schedule(new TimerTask() {
            @Override
            public void run() {
                ping();
            }
        }, 15000);
    }

    public void ping() {
        JSONObject pingBody = new JSONObject();
        pingBody.put("t", 8);
        send(pingBody.toString());
        startPingResolver();
    }

    public void send(String text) {
        parent.send(text);
    }

    public ProjZWebSocket(WebSocket parent) {
        this.pinger = new Timer();
        this.parent = parent;
        startPingResolver();
    }

    public static ProjZWebSocket create(HttpClient client) {
        Request request = new Request.Builder()
                .url("wss://ws.projz.com/v1/chat/ws")
                .get()
                .build();
        ProjZWebSocket webSocket = new ProjZWebSocket(null);
        webSocket.parent = client.newWebSocket(request, webSocket);
        return webSocket;
    }

}
