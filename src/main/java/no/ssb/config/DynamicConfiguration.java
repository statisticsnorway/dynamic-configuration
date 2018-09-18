package no.ssb.config;

public interface DynamicConfiguration {

    String evaluateToString(String key);

    int evaluateToInt(String key);

    boolean evaluateToBoolean(String key);
}
