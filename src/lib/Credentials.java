package lib;

import org.json.JSONObject;

public final class Credentials {
    String email;
    String password;
    String session = null;
    public Credentials setEmail(String email) {
        this.email = email;
        return this;
    }
    public Credentials setPassword(String password) {
        this.password = password;
        return this;
    }
    public Credentials setSession(String session) {
        this.session = session;
        return this;
    }
    public String getEmail() {
        return this.email;
    }
    public String getPassword() {
        return this.password;
    }

    public String getSession() {
        return this.session;
    }

    public String asJson() {
        JSONObject representation = new JSONObject();
        representation.put("email", email);
        representation.put("password", password);
        if (session != null) {
            representation.put("lastSessionId", session);
        }
        return representation.toString();
    }
}
