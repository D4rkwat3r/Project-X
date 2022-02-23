import lib.*;
import org.json.JSONArray;
import org.json.JSONObject;
import util.ProjZWebSocket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public final class Bot extends Thread {

    private Credentials credentials;
    private boolean stopped;
    private final HttpClient http;
    private final BotCallback onFailure;
    private final List<Signal> signals;
    private final Random random;
    private String uid = null;

    public Bot(Credentials credentials, BotCallback onFailure) {
        this.credentials = credentials;
        this.stopped = false;
        this.http = new HttpClient();
        this.onFailure = onFailure;
        this.signals = new ArrayList<>();
        this.random = new Random();
        this.http.addExtraHeader("rawDeviceId", HttpUtils.getInstance().deviceId());
        start();
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public Bot setCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Console.red(String.format("%s > Прерван", credentials.getEmail()));
                break;
            }
            if (signals.isEmpty()) {
                continue;
            }
            for (Signal signal : signals) {
                try {
                    executeSignal(signal);
                } catch (Exception e) {
                    Console.red(String.format("%s > Ошибка при запуске функции", credentials.getEmail()));
                    signal.notifyExecuted();
                }
            }
            signals.clear();
        }
    }

    private void executeSignal(Signal signal) throws Exception {
        switch (signal.getType()) {
            case AUTH:
                authenticate((boolean) signal.getDataPart("silentMode"));
                break;
            case JOIN_CHAT:
                joinChat((String) signal.getDataPart("url"));
                break;
            case LEAVE_CHAT:
                leaveChat((String) signal.getDataPart("url"));
                break;
            case JOIN_CIRCLE:
                joinCircle((String) signal.getDataPart("url"));
                break;
            case LEAVE_CIRCLE:
                leaveCircle((String) signal.getDataPart("url"));
                break;
            case SEND_MESSAGE:
                sendMessage((String) signal.getDataPart("url"), (String) signal.getDataPart("text"));
                break;
            case START_CHAT:
                startChat((String) signal.getDataPart("url"), (String) signal.getDataPart("text"));
                break;
            case CHANGE_NICKNAME:
                changeNickname((String) signal.getDataPart("newNickname"));
                break;
            case CREATE_BLOG:
                createBlog((String) signal.getDataPart("url"), (String) signal.getDataPart("content"), (String) signal.getDataPart("hexColor"));
                break;
            case STOP_THREAD:
                stopThread();
                break;
        }
        signal.notifyExecuted();
    }

    public Signal execute(Signal signal) {
        this.signals.add(signal);
        return signal;
    }

    public Signal executeNow(Signal signal) {
        try {
            executeSignal(signal);
        } catch (Exception e) {
            Console.red(String.format("%s > Ошибка при запуске функции", credentials.getEmail()));
            signal.notifyExecuted();
        }
        return signal;
    }

    private void authenticate(boolean silent) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("authType", 1);
        requestBody.put("email", credentials.getEmail());
        requestBody.put("password", credentials.getPassword());
        ResponseWrapper response = http.postJson("https://api.projz.com/v1/auth/login", requestBody.toString());
        if (response.isSucceed) {
            String sid = response.jsonBody.getString("sId");
            String uid = String.valueOf(response.jsonBody.getJSONObject("account").get("uid"));
            credentials.setSession(sid);
            http.addExtraHeader("sId", credentials.getSession());
            this.uid = uid;
            if (!silent) {
                Console.green(String.format("%s > Авторизован", credentials.getEmail()));
            }
            return;
        }
        String message = response.jsonBody.getString("apiMsg");
        Console.red(String.format("%s > Ошибка авторизации (%s)", credentials.getEmail(), message));
        onFailure.run(this);
    }

    private String getObjectId(String url) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("link", url);
        ResponseWrapper response = http.postJson("https://api.projz.com/v1/links/path", requestBody.toString());
        if (response.jsonBody.keySet().contains("objectId")) {
            String objectId = String.valueOf(response.jsonBody.get("objectId"));
            Console.green(String.format("%s > ID получен: %s", credentials.getEmail(), objectId));
            return objectId;
        }
        String message = response.jsonBody.getString("apiMsg");
        Console.red(String.format("%s > Ошибка при попытке получить ID (%s)", credentials.getEmail(), message));
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {}
        return null;
    }

    private void joinChat(String url) throws IOException {
        String objectId = getObjectId(url);
        if (objectId != null) {
            ResponseWrapper response = http.postEmpty(
                    String.format("https://api.projz.com/v1/chat/threads/%s/members", objectId)
            );
            if (response.isSucceed) {
                Console.green(String.format("%s > Вошёл в чат", credentials.getEmail()));
                return;
            }
            String message = response.jsonBody.getString("apiMsg");
            Console.red(String.format("%s > Ошибка при входе в чат (%s)", credentials.getEmail(), message));
        }
    }

    private void leaveChat(String url) throws IOException {
        String objectId = getObjectId(url);
        if (objectId != null) {
            ResponseWrapper response = http.delete(
                    String.format("https://api.projz.com/v1/chat/threads/%s/members", objectId)
            );
            if (response.isSucceed) {
                Console.green(String.format("%s > Покинул чат", credentials.getEmail()));
                return;
            }
            String message = response.jsonBody.getString("apiMsg");
            Console.red(String.format("%s > Ошибка при выходе из чата (%s)", credentials.getEmail(), message));
        }
    }

    private void joinCircle(String url) throws IOException {
        String objectId = getObjectId(url);
        if (objectId != null) {
            ResponseWrapper response = http.postEmpty(
                    String.format("https://api.projz.com/v1/circles/%s/members", objectId)
            );
            if (response.isSucceed) {
                Console.green(String.format("%s > Вошёл в сообщество", credentials.getEmail()));
                return;
            }
            String message = response.jsonBody.getString("apiMsg");
            Console.red(String.format("%s > Ошибка при входе в сообщество (%s)", credentials.getEmail(), message));
        }
    }

    public void leaveCircle(String url) throws IOException {
        String objectId = getObjectId(url);
        if (objectId != null) {
            ResponseWrapper response = http.delete(
                    String.format("https://api.projz.com/v1/circles/%s/members", objectId)
            );
            if (response.isSucceed) {
                Console.green(String.format("%s > Покинул сообщество", credentials.getEmail()));
                return;
            }
            String message = response.jsonBody.getString("apiMsg");
            Console.red(String.format("%s > Ошибка при выходе из сообщества (%s)", credentials.getEmail(), message));
        }
    }

    public void sendMessage(String url, String text) throws IOException {
        String objectId = getObjectId(url);
        if (objectId != null) {
            ProjZWebSocket webSocket = ProjZWebSocket.create(http);
            webSocket.send(String.format("{\"t\":1,\"threadId\":%d,\"msg\":{\"type\":1,\"status\":1,\"threadId\":%d,\"createdTime\":%d,\"uid\":%d,\"seqId\":%d,\"content\":\"%s\",\"messageId\":0,\"asSummary\":false,\"rolePlayMode\":0,\"onlineMemberCount\":0,\"roleList\":[],\"threadActivityId\":0,\"threadActivityType\":0,\"memberList\":[],\"applyCount\":0,\"userList\":[],\"extensions\":{\"friendshipLevel\":0,\"contentStatus\":1},\"parentId\":0,\"containerStatus\":0}}", Long.parseLong(objectId), Long.parseLong(objectId), System.currentTimeMillis(), Long.parseLong(uid), System.currentTimeMillis(), text));
            Console.green(String.format("%s > Осуществлена попытка отправки сообщения", credentials.getEmail()));
        }
    }

    public void startChat(String url, String text) throws IOException, InterruptedException {
        String objectId = getObjectId(url);
        if (objectId != null) {
            JSONArray uidList = new JSONArray();
            uidList.put(Long.parseLong(objectId));
            JSONObject requestBody = new JSONObject();
            requestBody.put("type", 1);
            requestBody.put("status", 1);
            requestBody.put("background", http.postMultipartFile("https://api.projz.com/v1/media/upload?target=1&duration=0", getClass().getResourceAsStream("bg.jpg")));
            requestBody.put("inviteMessageContent", text);
            requestBody.put("invitedUids", uidList);
            TimeUnit.SECONDS.sleep(random.nextInt(5));
            ResponseWrapper response = http.postJson("https://api.projz.com/v1/chat/threads", requestBody.toString());
            if (response.isSucceed) {
                Console.green(String.format("%s > Чат создан", credentials.getEmail()));
                return;
            }
            String message = response.jsonBody.getString("apiMsg");
            Console.red(String.format("%s > Ошибка при создании чата (%s)", credentials.getEmail(), message));
        }
    }

    public void changeNickname(String newNickname) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("uid", 0);
        requestBody.put("status", 1);
        requestBody.put("lastActiveTime", 0);
        requestBody.put("nickname", newNickname);
        requestBody.put("managerAbsencesDays", 0);
        requestBody.put("chatSenderAllowed", false);
        ResponseWrapper response = http.postJson(String.format("https://api.projz.com/v1/users/profile/%d/update-profile", Long.parseLong(uid)), requestBody.toString());
        if (response.isSucceed) {
            Console.green(String.format("%s > Никнейм изменён", credentials.getEmail()));
            return;
        }
        String message = response.jsonBody.getString("apiMsg");
        Console.red(String.format("%s > Не удалось изменить никнейм (%s)", credentials.getEmail(), message));
    }

    public void createBlog(
            String url,
            String content,
            String hexColor) throws IOException {
        String object_id = getObjectId(url);
        if (object_id != null) {
            JSONArray mediaList = new JSONArray();
            JSONArray circleIdList = new JSONArray();
            JSONArray folderAffiliationList = new JSONArray();
            JSONObject extensions = new JSONObject();
            String color = "";
            if (!hexColor.startsWith("#")) {
                color = "#" + hexColor;
            } else {
                color = hexColor;
            }
            extensions.put("contentStatus", 1);
            extensions.put("commentDisabled", false);
            extensions.put("backgroundColor", color);
            circleIdList.put(Long.parseLong(object_id));
            JSONObject folderAffiliation = new JSONObject();
            folderAffiliation.put("circleId", Long.parseLong(object_id));
            folderAffiliation.put("folderIdList", new JSONArray());
            folderAffiliation.put("folderList", new JSONArray());
            folderAffiliationList.put(folderAffiliation);
            mediaList.put(http.postMultipartFile("https://api.projz.com/v1/media/upload?target=3&duration=0", getClass().getResourceAsStream("tree.jpg")));
            JSONObject requestBody = new JSONObject();
            requestBody.put("type", 2);
            requestBody.put("content", content);
            requestBody.put("mediaList", mediaList);
            requestBody.put("circleIdList", circleIdList);
            requestBody.put("folderAffiliationList", folderAffiliationList);
            requestBody.put("extensions", extensions);
            ResponseWrapper response = http.postJson("https://api.projz.com/v1/blogs", requestBody.toString());
            if (response.isSucceed) {
                Console.green(String.format("%s > Пост создан", credentials.getEmail()));
                return;
            }
            String message = response.jsonBody.getString("apiMsg");
            Console.red(String.format("%s > Не удалось создать пост (%s)", credentials.getEmail(), message));
        }
    }

    private void stopThread() {
        stopped = true;
        Console.cyan(String.format("%s > Поток останавливается", credentials.getEmail()));
    }

}
