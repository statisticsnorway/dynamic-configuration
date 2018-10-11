package no.ssb.config;

import java.util.Map;

public interface DynamicConfiguration {

    String evaluateToString(String key);

    int evaluateToInt(String key);

    boolean evaluateToBoolean(String key);

    Map<String,String> asMap();

}
