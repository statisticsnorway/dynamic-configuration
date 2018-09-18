# Configuration

The Configuration library is a clean (no external dependencies) Java implementation inspired by and partly based on work
found in the Constretto library: https://github.com/constretto/constretto-core

This library contains a simple configuration loading and override mechanism capable of reading properties files from 
file-system or classpath and also ability to read configuration from environment and java system properties.


Example:
```properties
# application.properties
host=localhost
port=28282
accesslog.enabled=true
```
```java
// Sample Java application using configuration library

public class MyApp {

    public static void main(String[] args) {

        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource("application.properties")
                .propertiesResource("application_override.properties")
                .environment("MYAPP_")
                .systemProperties()
                .build();

        String host = configuration.evaluateToString("host");
        int port = configuration.evaluateToInt("port");
        boolean accessLogEnabled = configuration.evaluateToBoolean("accesslog.enabled");

        new MyWebServer(host, port, accessLogEnabled);
    }
}
```
