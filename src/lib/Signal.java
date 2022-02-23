package lib;

import java.util.HashMap;
import java.util.Map;

public class Signal {
    long enqueuedTime;
    private Map<String, Object> data;
    private SignalType type;
    volatile boolean isExecuted;

    public Signal(SignalType type) {
        this.enqueuedTime = System.currentTimeMillis();
        this.data = new HashMap<>();
        this.type = type;
    }

    public SignalType getType() {
        return type;
    }

    public Signal addDataPart(String key, Object dataPart) {
        data.put(key, dataPart);
        return this;
    }

    public Object getDataPart(String key) {
        return data.get(key);
    }

    public void waitFor() {
        while (!isExecuted) {
            Thread.onSpinWait();
        }
    }

    public void notifyExecuted() {
        isExecuted = true;
    }

    public boolean isExecuted() {
        return isExecuted;
    }
}
