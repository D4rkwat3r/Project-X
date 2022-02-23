import lib.*;
import org.json.JSONObject;
import util.RuCaptcha;
import util.SecMail;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class AutoReg {

    private HttpClient http;
    private RuCaptcha ruCaptcha;
    private ThreadLocalRandom localRandom;


    public AutoReg(HttpClient http, RuCaptcha ruCaptcha) {
        this.http = http;
        this.ruCaptcha = ruCaptcha;
        this.localRandom = ThreadLocalRandom.current();
    }

    private boolean checkSecurityValidation(SecMail mail, String code) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("authType", 1);
        requestBody.put("email", mail.getFullAddress());
        requestBody.put("securityCode", code);
        ResponseWrapper response = http.postJson("https://api.projz.com/v1/auth/check-security-validation", requestBody.toString());
        return response.isSucceed;
    }

    public void verifyReCaptcha(SecMail mail, String userVerify) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("captchaValue", userVerify);
        ResponseWrapper response = http.postJson("https://www.projz.com/api/f/v1/risk/verify-captcha", requestBody.toString());
        if (response.jsonBody.getBoolean("success")) {
            Console.green(String.format("АВТОРЕГ (%s) > ReCaptcha пройдена", mail.getFullAddress()));
            return;
        }
        Console.red(String.format("АВТОРЕГ (%s) > ReCaptcha провалена", mail.getFullAddress()));
    }

    private String securityValidation(SecMail mail, RuCaptcha ruCaptcha) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("authType", 1);
            requestBody.put("purpose", 1);
            requestBody.put("email", mail.getFullAddress());
            requestBody.put("countryCode", "en");
            ResponseWrapper response = http.postJson("https://api.projz.com/v1/auth/request-security-validation", requestBody.toString());
            if (response.isSucceed) {
                String solving = ruCaptcha.solveImageCaptcha(mail.getUrl());
                if (!checkSecurityValidation(mail, solving)) {
                    Console.red(String.format("АВТОРЕГ (%s) > Проверка кода провалена, пробуем ещё раз...", mail.getFullAddress()));
                    return securityValidation(mail, ruCaptcha);
                }
                Console.green(String.format("АВТОРЕГ (%s) > Проверка кода выполнена", mail.getFullAddress()));
                return solving;
            }
            String message = response.jsonBody.getString("apiMsg");
            Console.red(String.format("АВТОРЕГ (%s) > Ошибка запроса на проверку кода (%s)", mail.getFullAddress(), message));
        } catch (IOException | InterruptedException e) {
            Console.red(String.format("АВТОРЕГ (%s) > Проверка кода провалена из-за исключения", mail.getFullAddress()));
        } catch (IllegalStateException e) {
            Console.red("Нулевой баланс на RuCaptcha, невозможно декодировать капчу");
        }
        return null;
    }

    public Bot createNewBot(
            String nickname,
            String password,
            RuCaptcha ruCaptcha,
            HttpClient client,
            BotCallback onFailure,
            SecMail secMail,
            String verCode
    ) {
        this.http.replaceExtraHeader("rawDeviceId", HttpUtils.getInstance().deviceId());
        SecMail mail = secMail;
        String code = verCode;
        try {
            TimeUnit.SECONDS.sleep(localRandom.nextInt(1, 6));
            if (mail == null) {
                mail = SecMail.generate();
            }
            if (code == null) {
                code = securityValidation(mail, ruCaptcha);
            }
            Bot newBot = new Bot(new Credentials().setEmail(mail.getFullAddress()).setPassword(password), onFailure);
            JSONObject requestData = new JSONObject();
            requestData.put("authType", 1);
            requestData.put("purpose", 1);
            requestData.put("email", mail.getFullAddress());
            requestData.put("password", password);
            requestData.put("securityCode", code);
            requestData.put("nickname", nickname);
            requestData.put("tagLine", "Project X");
            requestData.put("icon", client.postMultipartFile("https://api.projz.com/v1/media/upload?target=1&duration=0", getClass().getResourceAsStream("triangle.jpg")));
            requestData.put("nameCardBackground", client.postMultipartFile("https://api.projz.com/v1/media/upload?target=11&duration=0", getClass().getResourceAsStream("triangle_bg.jpg")));
            requestData.put("gender", 1);
            requestData.put("birthday", "1900-01-01");
            requestData.put("countryCode", "en");
            requestData.put("suggestedCountryCode", "EN");
            ResponseWrapper response = client.postJson("https://api.projz.com/v1/auth/register", requestData.toString());
            if (response.isSucceed) {
                Console.green(String.format("АВТОРЕГ (%s) > Зарегистрирован", mail.getFullAddress()));
                newBot.executeNow(new Signal(SignalType.AUTH).addDataPart("silentMode", true));
                TimeUnit.SECONDS.sleep(localRandom.nextInt(1, 6));
                return newBot;
            } else if (Objects.equals(response.jsonBody.getString("apiMsg"), "captcha caught")) {
                Console.cyan(String.format("АВТОРЕГ (%s) > Решаем капчу...", mail.getFullAddress()));
                String token = ruCaptcha.solveReCaptcha(response.jsonBody.getString("redirectUrl"));
                verifyReCaptcha(mail, token);
                return createNewBot(nickname, password, ruCaptcha, client, onFailure, mail, code);
            } else if (Objects.equals(response.jsonBody.getString("apiMsg"), "The verification code is incorrect. Please try again.")) {
                Console.cyan(String.format("АВТОРЕГ (%s) > Неверный код подтверждения, пробуем снова...", mail.getFullAddress()));
                return createNewBot(nickname, password, ruCaptcha, client, onFailure, null, null);
            }
            else {
                String message = response.jsonBody.getString("apiMsg");
                Console.red(String.format("АВТОРЕГ (%s) > Регистрация провалена (%s)", mail.getFullAddress(), message));
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalStateException e) {
            Console.red("Нулевой баланс на RuCaptcha, невозможно декодировать капчу");
        }
        return null;
    }
}
