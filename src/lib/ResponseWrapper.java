package lib;

import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public final class ResponseWrapper {

    public int code;
    public String body;
    public boolean isSucceed;
    public JSONObject jsonBody;

    private ResponseWrapper(int code, String body, boolean isSucceed) {
        this.code = code;
        this.body = body;
        this.isSucceed = isSucceed;
        this.jsonBody = new JSONObject(this.body);
    }

    public static ResponseWrapper wrap(Response response) {
        ResponseWrapper wrapper = null;
        try {
            wrapper = new ResponseWrapper(response.code(), response.body().string(), response.isSuccessful());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return wrapper;
    }
}
