import lib.*;
import org.json.JSONObject;
import util.RuCaptcha;
import util.SQLITE;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Controller {

    private final List<Bot> bots;
    private final HttpClient client;
    private final RuCaptcha ruCaptcha;
    private final AutoReg autoReg;
    private SQLITE sqlite;


    public Controller(String ruCaptchaToken, String projectZCaptchaToken) {
        this.bots = new ArrayList<>();
        this.client = new HttpClient();
        this.ruCaptcha = new RuCaptcha(ruCaptchaToken, projectZCaptchaToken);
        this.autoReg = new AutoReg(this.client, this.ruCaptcha);
        this.sqlite = new SQLITE();
    }

    public int getBotsCount() {
        return bots.size();
    }

    private void waitAll(List<Signal> signals) {
        boolean finished = false;
        while (!finished) {
            List<Boolean> states = new ArrayList<>();
            for (Signal signal : signals) {
                states.add(signal.isExecuted());
            }
            finished = !states.contains(false);
        }
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            waitAll(signals);
        }
    }

    private List<Signal> enqueueAll(Signal signalTemplate) {
        List<Signal> signals = new ArrayList<>();
        for (Bot bot : bots) {
            signals.add(bot.execute(signalTemplate));
        }
        return signals;
    }

    public void loadFromDB() {
        try {
            for (Credentials credentials : sqlite.readAccounts()) {
                Bot bot = new Bot(credentials, (info) -> {
                    Console.red(String.format("%s > Непредвиденная критическая ошибка", info.getCredentials().getEmail()));
                    info.executeNow(new Signal(SignalType.STOP_THREAD));
                    bots.remove(info);
                });
                Console.green(String.format("Загружен аккаунт %s", credentials.getEmail()));
                bots.add(bot);
            }
            waitAll(enqueueAll(new Signal(SignalType.AUTH).addDataPart("silentMode", false)));
        } catch (SQLException e) {
            Console.red("CONTROLLER > Загрузка из базы данных провалена");
            System.exit(1);
        }
    }

    public void startAutoReg(String nickname, String password, int seconds) {
        long targetedTimestamp = Instant.now().getEpochSecond() + seconds;
        for (int i = 0; i <= 15; i++) {
            int threadNumber = i;
            new Thread(() -> {
                Console.green(String.format("%d поток авторега запущен", threadNumber));
                while (Instant.now().getEpochSecond() < targetedTimestamp) {
                    Bot bot = autoReg.createNewBot(
                            nickname,
                            password,
                            ruCaptcha,
                            client,
                            (info) -> Console.red(String.format("АВТОРЕГ (%s) > Ошибка", info.getCredentials().getEmail())),
                            null,
                            null
                    );
                    if (bot != null) {
                        sqlite.writeAccount(bot.getCredentials());
                        Console.green(String.format("АВТОРЕГ (%s) > Аккаунт записан в базу данных", bot.getCredentials().getEmail()));
                    }
                }
                Console.cyan(String.format("Работа %d потока авторега завершена из-за истечения времени", threadNumber));
            }).start();
        }
    }

    public void broadcastJoinChatRequest(String url) {
        waitAll(enqueueAll(new Signal(SignalType.JOIN_CHAT).addDataPart("url", url)));
    }

    public void broadcastLeaveChatRequest(String url) {
        waitAll(enqueueAll(new Signal(SignalType.LEAVE_CHAT).addDataPart("url", url)));
    }

    public void broadcastJoinCircleRequest(String url) {
        waitAll(enqueueAll(new Signal(SignalType.JOIN_CIRCLE).addDataPart("url", url)));
    }

    public void broadcastLeaveCircleRequest(String url) {
        waitAll(enqueueAll(new Signal(SignalType.LEAVE_CIRCLE).addDataPart("url", url)));
    }

    public void broadcastMessageSendingRequest(String url, String text) {
        waitAll(enqueueAll(new Signal(SignalType.SEND_MESSAGE).addDataPart("url", url).addDataPart("text", text)));
    }

    public void broadcastStartChatRequest(String url, String text) {
        waitAll(enqueueAll(new Signal(SignalType.START_CHAT).addDataPart("url", url).addDataPart("text", text)));
    }

    public void broadcastChangeNicknameRequest(String newNickname) {
        waitAll(enqueueAll(new Signal(SignalType.CHANGE_NICKNAME).addDataPart("newNickname", newNickname)));
    }

    public void broadcastCreateBlogRequest(String url, String content, String hexColor) {
        waitAll(enqueueAll(new Signal(SignalType.CREATE_BLOG).addDataPart("url", url).addDataPart("content", content).addDataPart("hexColor", hexColor)));
    }

    public void reauthorizeBots() {
        waitAll(enqueueAll(new Signal(SignalType.AUTH).addDataPart("silentMode", false)));
    }

}
