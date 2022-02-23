package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ConfigUtils {
    public static void writeConfigFile(String ruCaptchaKey, String reCaptchaToken) throws IOException {
        File config = new File("ProjX.conf");
        config.createNewFile();
        FileOutputStream stream = new FileOutputStream(config);
        stream.write(String.format("RUCAPTCHA:%s\n", ruCaptchaKey).getBytes(StandardCharsets.UTF_8));
        stream.write(String.format("RECAPTCHA:%s", reCaptchaToken).getBytes(StandardCharsets.UTF_8));
    }
    public static boolean isConfigured() {
        return new File("ProjX.conf").exists();
    }
    public static Config readConfigFile() throws IOException {
        File config = new File("ProjX.conf");
        FileInputStream stream = new FileInputStream(config);
        String[] configParams = new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(Pattern.quote("\n"));
        return new Config(configParams[0].split(Pattern.quote(":"))[1], configParams[1].split(Pattern.quote(":"))[1]);
    }
}
