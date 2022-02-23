package lib;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;

public final class Console {
    static {
        AnsiConsole.systemInstall();
    }
    public static void red(String text) {
        System.out.println(ansi().fg(RED).a(text).reset());
    }
    public static void green(String text) {
        System.out.println(ansi().fg(GREEN).a(text).reset());
    }
    public static void cyan(String text) {
        System.out.println(ansi().fg(CYAN).a(text).reset());
    }
    public static void blue(String text) {
        System.out.println(ansi().fg(BLUE.value() + 10).a(text).reset());
    }
    public static void magenta(String text) {
        System.out.println(ansi().fg(MAGENTA).a(text).reset());
    }
    public static void white(String text) {
        System.out.println(ansi().fg(WHITE).a(text).reset());
    }
    public static void clear() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                return;
            }
            Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException e) {
            clear();
        }
    }
    public static void renderMenu() {
        for (int i = 0; i < SignalType.values().length; i++) {
            switch (SignalType.values()[i]) {
                case AUTH:
                    cyan(String.format("%d. Авторизовать всех ботов ещё раз", i + 1));
                    break;
                case SPAM_CHAT:
                    cyan(String.format("%d. Войти в чат и начать на него атаку (нельзя прервать)", i + 1));
                    break;
                case STOP_THREAD:
                    cyan(String.format("%d. Остановить потоки всех ботов", i + 1));
                    break;
                case JOIN_CHAT:
                    cyan(String.format("%d. Войти в чат", i + 1));
                    break;
                case LEAVE_CHAT:
                    cyan(String.format("%d. Покинуть чат", i + 1));
                    break;
                case START_CHAT:
                    cyan(String.format("%d. Начать разговор с пользователем", i + 1));
                    break;
                case CHANGE_NICKNAME:
                    cyan(String.format("%d. Поменять никнеймы", i + 1));
                    break;
                case CREATE_BLOG:
                    cyan(String.format("%d. Создать пост", i + 1));
                    break;
                case JOIN_CIRCLE:
                    cyan(String.format("%d. Войти в сообщество", i + 1));
                    break;
                case LEAVE_CIRCLE:
                    cyan(String.format("%d. Покинуть сообщество", i + 1));
                    break;
                case SEND_MESSAGE:
                    cyan(String.format("%d. Отправить сообщение", i + 1));
                    break;
            }
        }
        cyan(String.format("%d. Запустить авторег", SignalType.values().length + 1));
    }
    public static void renderLabel() {
        blue("    ____               _           __     _  __\n" +
                "   / __ \\_________    (_)__  _____/ /_   | |/ /\n" +
                "  / /_/ / ___/ __ \\  / / _ \\/ ___/ __/   |   / \n" +
                " / ____/ /  / /_/ / / /  __/ /__/ /_    /   |  \n" +
                "/_/   /_/   \\____/_/ /\\___/\\___/\\__/   /_/|_|  \n" +
                "                /___/                          ");
    }
}
