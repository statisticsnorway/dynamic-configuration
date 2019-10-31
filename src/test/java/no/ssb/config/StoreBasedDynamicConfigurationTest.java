package no.ssb.config;

import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

public class StoreBasedDynamicConfigurationTest {

    @Test
    public void thatClasspathPropertiesAreAddedToMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder().propertiesResource("storebaseddynamicconfigurationtest.properties").build().asMap();
        assertEquals(map.get("IamKey"), "I'm a value");
    }

    @Test
    public void thatFilePropertiesAreAddedToMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder().propertiesResource("src/test/resources/anotherfile.properties").build().asMap();
        assertEquals(map.get("IamKey"), "I'm another value");
    }

    @Test
    public void thatEnvironmentWithEmptyPrefixIsAddedToMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder().environment("").build().asMap();
        Map<String, String> env = System.getenv();
        assertEquals(map, env);
        assertNotNull(map.get("PATH"));
        assertEquals(map.get("PATH"), env.get("PATH"));
    }

    @Test
    public void thatEnvironmentWithPATPrefixIsAddedToMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder().environment("PAT").build().asMap();
        Map<String, String> env = System.getenv();
        assertNotNull(map.get("H"));
        assertEquals(map.get("H"), env.get("PATH"));
    }

    @Test
    public void thatSystemPropertiesAreAddedToMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder().systemProperties().build().asMap();
        Properties properties = System.getProperties();
        Map<String, String> systemProperties = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            systemProperties.put((String) entry.getKey(), (String) entry.getValue());
        }
        assertEquals(map, systemProperties);
        assertNotNull(map.get("user.dir"));
        assertEquals(map.get("user.dir"), systemProperties.get("user.dir"));
    }

    @Test
    public void thatHardcodedValuesAreAddedToMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder().values("hardcodedKey", "Hardcoded Value").build().asMap();
        assertEquals(map.get("hardcodedKey"), "Hardcoded Value");
    }

    @Test
    public void thatHardcodedStoreIsLastIsMaintainedInMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder()
                .environment("")
                .systemProperties()
                .propertiesResource("storebaseddynamicconfigurationtest.properties")
                .propertiesResource("src/test/resources/anotherfile.properties")
                .values("user.dir", "hardcoded-working-directory")
                .build().asMap();
        assertEquals(map.get("user.dir"), "hardcoded-working-directory");
    }

    @Test
    public void thatFileStoreIsLastIsMaintainedInMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder()
                .environment("")
                .systemProperties()
                .propertiesResource("storebaseddynamicconfigurationtest.properties")
                .values("user.dir", "hardcoded-working-directory")
                .propertiesResource("src/test/resources/anotherfile.properties")
                .build().asMap();
        assertEquals(map.get("user.dir"), "anothertestuserdir");
    }

    @Test
    public void thatClasspathStoreIsLastIsMaintainedInMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder()
                .environment("")
                .systemProperties()
                .propertiesResource("src/test/resources/anotherfile.properties")
                .values("user.dir", "hardcoded-working-directory")
                .propertiesResource("storebaseddynamicconfigurationtest.properties")
                .build().asMap();
        assertEquals(map.get("user.dir"), "storebasedpathtestuserdir");
    }

    @Test
    public void thatSystemPropertiesStoreIsLastIsMaintainedInMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder()
                .environment("")
                .propertiesResource("src/test/resources/anotherfile.properties")
                .values("user.dir", "hardcoded-working-directory")
                .propertiesResource("storebaseddynamicconfigurationtest.properties")
                .systemProperties()
                .build().asMap();
        assertEquals(map.get("user.dir"), System.getProperty("user.dir"));
    }

    @Test
    public void thatEnvironmentStoreIsLastIsMaintainedInMap() {
        Map<String, String> map = new StoreBasedDynamicConfiguration.Builder()
                .systemProperties()
                .propertiesResource("src/test/resources/anotherfile.properties")
                .values("user.dir", "hardcoded-working-directory")
                .propertiesResource("storebaseddynamicconfigurationtest.properties")
                .environment("")
                .build().asMap();
        assertEquals(map.get("PATH"), System.getenv("PATH"));
    }

    @Test
    public void thatStoreBasedDynamicConfigurationIsDeepCopied() {
        StoreBasedDynamicConfiguration.Builder builder = new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource("storebaseddynamicconfigurationtest.properties");

        StoreBasedDynamicConfiguration.Builder copyBuilder = builder.copy();
        assertEquals(builder, copyBuilder);

        builder.values("foo", "bar");
        assertNotEquals(builder, copyBuilder);

        copyBuilder.values("foo", "bar");
        assertEquals(builder, copyBuilder);

        builder.values("IamKey", "I'm a value");
        assertNotEquals(builder, copyBuilder);

        copyBuilder.values("IamKey", "I'm a value");
        assertEquals(builder, copyBuilder);
    }
}
