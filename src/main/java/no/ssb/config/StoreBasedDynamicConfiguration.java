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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class StoreBasedDynamicConfiguration implements DynamicConfiguration {

    private final List<Store> storeList;

    private StoreBasedDynamicConfiguration(List<Store> storeList) {
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

    private interface Store {
        /**
         * @param key
         * @return the value or null if this store has no value configured for key.
         */
        String get(String key);
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
        final List<String> propertyResourcePathList = new ArrayList<>();
        boolean systemProperties = false;
        boolean environment = false;
        String environmentPrefix;
        final List<String[]> valuesList = new ArrayList<>();

        public Builder propertiesResource(String resourcePath) {
            propertyResourcePathList.add(resourcePath);
            return this;
        }

        public Builder environment(String prefix) {
            environment = true;
            environmentPrefix = prefix;
            return this;
        }

        public Builder systemProperties() {
            systemProperties = true;
            return this;
        }

        public Builder values(String... keyValuePairs) {
            valuesList.add(keyValuePairs);
            return this;
        }

        public StoreBasedDynamicConfiguration build() {
            LinkedList<Store> storeList = new LinkedList<>();
            for (String resourcePath : propertyResourcePathList) {
                storeList.addFirst(new PropertiesStore(resourcePath));
            }
            if (environment) {
                storeList.addFirst(new EnvironmentStore(environmentPrefix));
            }
            if (systemProperties) {
                storeList.addFirst(new SystemPropertiesStore());
            }
            for (String[] keyValuePairs : valuesList) {
                storeList.addFirst(new HardcodedStore(keyValuePairs));
            }
            return new StoreBasedDynamicConfiguration(storeList);
        }
    }
}
