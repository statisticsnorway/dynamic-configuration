package no.ssb.config;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class StoreBasedDynamicConfiguration implements DynamicConfiguration {

    private final Deque<Store> storeList;

    private StoreBasedDynamicConfiguration(Deque<Store> storeList) {
        this.storeList = storeList;
    }

    @Override
    public String evaluateToString(String key) {
        Iterator<Store> it = storeList.iterator();
        while (it.hasNext()) {
            Store store = it.next();
            String value = store.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public int evaluateToInt(String key) {
        return Integer.parseInt(evaluateToString(key));
    }

    @Override
    public boolean evaluateToBoolean(String key) {
        return Boolean.parseBoolean(evaluateToString(key));
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> map = new LinkedHashMap<>();
        Iterator<Store> it = storeList.descendingIterator();
        while (it.hasNext()) {
            Store store = it.next();
            store.putAllToMap(map);
        }
        return map;
    }

    private interface Store {
        /**
         * @param key
         * @return the value or null if this store has no value configured for key.
         */
        String get(String key);

        void putAllToMap(Map<String, String> map);
    }

    private static class PropertiesStore implements Store {
        private final String resourcePath;
        private final Map<String, String> propertyByName = new LinkedHashMap<>();

        private Iterable<URL> getPropertyResources() {
            List<URL> resources = new ArrayList<>();
            Enumeration<URL> classPathResources;
            try {
                classPathResources = ClassLoader.getSystemResources(resourcePath);
                while (classPathResources.hasMoreElements()) {
                    resources.add(classPathResources.nextElement());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return resources;
        }

        private PropertiesStore(String resourcePath) {
            this.resourcePath = resourcePath;
            // If classpath resource exists, read it
            Iterable<URL> classPathResources = getPropertyResources();
            classPathResources.forEach(classPathResource -> {
                Properties properties = new Properties();
                try {
                    URLConnection urlConnection = classPathResource.openConnection();
                    try (Reader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))) {
                        properties.load(reader);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                for (Map.Entry<Object, Object> e : properties.entrySet()) {
                    propertyByName.put((String) e.getKey(), (String) e.getValue());
                }
            });

            // If file exists, override configuration
            Path path = Paths.get(resourcePath);
            if (Files.exists(path)) {
                Properties properties = new Properties();
                try (Reader reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
                    properties.load(reader);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                for (Map.Entry<Object, Object> e : properties.entrySet()) {
                    propertyByName.put((String) e.getKey(), (String) e.getValue());
                }
            }

        }

        public String get(String key) {
            return propertyByName.get(key);
        }

        @Override
        public void putAllToMap(Map<String, String> map) {
            map.putAll(propertyByName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PropertiesStore that = (PropertiesStore) o;
            return Objects.equals(resourcePath, that.resourcePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourcePath);
        }
    }

    private static class EnvironmentStore implements Store {
        private final String prefix;

        private EnvironmentStore(String prefix) {
            this.prefix = prefix;
        }

        public String get(String key) {
            return System.getenv(prefix + key);
        }

        @Override
        public void putAllToMap(Map<String, String> map) {
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    map.put(entry.getKey().substring(prefix.length()), entry.getValue());
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EnvironmentStore that = (EnvironmentStore) o;
            return Objects.equals(prefix, that.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix);
        }
    }

    private static class SystemPropertiesStore implements Store {
        private SystemPropertiesStore() {
        }

        public String get(String key) {
            return System.getProperty(key);
        }

        @Override
        public void putAllToMap(Map<String, String> map) {
            for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                map.put((String) entry.getKey(), (String) entry.getValue());
            }
        }

        @Override
        public int hashCode() {
            return 31 * 7385;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && getClass() == o.getClass();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreBasedDynamicConfiguration that = (StoreBasedDynamicConfiguration) o;
        return Objects.equals(storeList, that.storeList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeList);
    }

    private static class HardcodedStore implements Store {
        final Map<String, String> valueByKey = new LinkedHashMap<>();

        private HardcodedStore(String[] hardcodedValues) {
            for (int i = 0; i < hardcodedValues.length; i += 2) {
                valueByKey.put(hardcodedValues[i], hardcodedValues[i + 1]);
            }
        }

        public String get(String key) {
            return valueByKey.get(key);
        }

        @Override
        public void putAllToMap(Map<String, String> map) {
            map.putAll(valueByKey);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HardcodedStore that = (HardcodedStore) o;
            return Objects.equals(valueByKey, that.valueByKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(valueByKey);
        }
    }

    public static class Builder {
        final Deque<Store> storeList = new LinkedList<>();

        public Builder propertiesResource(String resourcePath) {
            storeList.addFirst(new PropertiesStore(resourcePath));
            return this;
        }

        public Builder environment(String prefix) {
            storeList.addFirst(new EnvironmentStore(prefix));
            return this;
        }

        public Builder systemProperties() {
            storeList.addFirst(new SystemPropertiesStore());
            return this;
        }

        public Builder values(String... keyValuePairs) {
            storeList.addFirst(new HardcodedStore(keyValuePairs));
            return this;
        }

        public StoreBasedDynamicConfiguration build() {
            return new StoreBasedDynamicConfiguration(storeList);
        }
    }
}
