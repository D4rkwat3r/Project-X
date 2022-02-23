import lib.Console;
import lib.Signal;
import lib.SignalType;
import util.Config;
import util.ConfigUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class Main {

    public static void resolveUserSelectedValue(int value, Scanner scanner, Controller controller) {
        if (value > SignalType.values().length + 1|| value < 1) {
            Console.red("Неверный выбор, попробуйте ещё раз\n");
            System.out.print("> ");
            resolveUserSelectedValue(scanner.nextInt(), scanner, controller);
        }
        switch (value) {
            case 1:
                controller.reauthorizeBots();
                break;
            case 2:
                Console.blue("Введите ссылку на чат\n");
                System.out.print("> ");
                controller.broadcastJoinChatRequest(System.console().readLine());
                break;
            case 3:
                Console.blue("Введите ссылку на чат\n");
                System.out.print("> ");
                controller.broadcastLeaveChatRequest(System.console().readLine());
                break;
            case 4:
                Console.blue("Введите ссылку на сообщество\n");
                System.out.print("> ");
                controller.broadcastJoinCircleRequest(System.console().readLine());
                break;
            case 5:
                Console.blue("Введите ссылку на сообщество\n");
                System.out.print("> ");
                controller.broadcastLeaveCircleRequest(System.console().readLine());
                break;
            case 6:
                Console.blue("Введите текст сообщения\n");
                System.out.print("> ");
                String text = System.console().readLine();
                Console.blue("Введите ссылку на чат\n");
                System.out.print("> ");
                controller.broadcastMessageSendingRequest(System.console().readLine(), text);
                break;
            case 7:
                Console.blue("Введите текст сообщения\n");
                System.out.print("> ");
                String inviteMessage = System.console().readLine();
                Console.blue("Введите ссылку на пользователя\n");
                System.out.print("> ");
                controller.broadcastStartChatRequest(System.console().readLine(), inviteMessage);
                break;
            case 8:
                Console.blue("Введите новый никнейм\n");
                System.out.print("> ");
                controller.broadcastChangeNicknameRequest(System.console().readLine());
                break;
            case 9:
                Console.blue("Введите ссылку на сообщество\n");
                System.out.print("> ");
                String link = System.console().readLine();
                Console.blue("Введите текст постов\n");
                System.out.print("> ");
                String content = System.console().readLine();
                Console.blue("Введите HEX-идентификатор фонового цвета постов\n");
                System.out.print("> ");
                String hexColor = System.console().readLine();
                controller.broadcastCreateBlogRequest(link, content, hexColor);
                break;
            case 12:
                Console.blue("Введите ник аккаунтов\n");
                System.out.print("> ");
                String nickname = System.console().readLine();
                Console.blue("Введите пароль аккаунтов\n");
                String password = System.console().readLine();
                Console.blue("Сколько секунд?\n");
                System.out.print("> ");
                controller.startAutoReg(nickname, password, scanner.nextInt());
                break;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        if (!ConfigUtils.isConfigured()) {
            Console.white("RuCaptcha не настроена. Программа не может работать без API ключа, " +
                    "но вы можете ввести значение-заглушку или ключ от аккаунта с нулевым балансом, " +
                    "если вам не нужен авторег");
            System.out.print("API KEY > ");
            String key = scanner.next();
            try {
                ConfigUtils.writeConfigFile(key, "6Lf_TS8eAAAAAI_HlQtAhFD0YO7yyKhryVPH8eYq");
            } catch (IOException e) {
                Console.red("Ошибка при записи в файл конфигурации");
                return;
            }
        }
        Config config;
        Controller controller;
        try {
            config = ConfigUtils.readConfigFile();
            controller = new Controller(config.getRuCaptchaKey(), config.getReCaptchaToken());
            Console.clear();
        } catch (IOException e) {
            e.printStackTrace();
            Console.red("Ошибка при чтении файла конфигурации");
            return;
        }
        controller.loadFromDB();
        String VERSION = "0.1";
        while (true) {
            Console.clear();
            Console.renderLabel();
            Console.white(String.format("\nProject X Botnet version: %s", VERSION));
            Console.magenta(String.format("Загружено аккаунтов: %d\n", controller.getBotsCount()));
            Console.renderMenu();
            Console.blue("\n\nВаш выбор\n");
            System.out.print("> ");
            resolveUserSelectedValue(scanner.nextInt(), scanner, controller);
        }
    }
}
