package com.bazaarvoice.soa;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HostAndPort;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ServiceInstanceTest {
    private static final int MAX_PAYLOAD_SIZE_IN_CHARACTERS = ServiceInstance.MAX_PAYLOAD_SIZE_IN_CHARACTERS;
    private static final DateTimeFormatter ISO8601 = ServiceInstance.ISO8601;
    private static final HostAndPort SERVER = HostAndPort.fromParts("server", 8080);

    @Test
    public void testInvalidServiceNames() {
        ServiceInstanceBuilder base = new ServiceInstanceBuilder();

        assertThrows(base.withName("Foo$Bar"), IllegalArgumentException.class);
        assertThrows(base.withName("%"), IllegalArgumentException.class);
        assertThrows(base.withName("a:b"), IllegalArgumentException.class);
        assertThrows(base.withName("a@b"), IllegalArgumentException.class);
        assertThrows(base.withName("!"), IllegalArgumentException.class);
        assertThrows(base.withName(null), IllegalArgumentException.class);
        assertThrows(base.withName(""), IllegalArgumentException.class);
    }

    @Test
    public void testInvalidAddresses() {
        ServiceInstanceBuilder base = new ServiceInstanceBuilder();

        assertThrows(base.withAddress(HostAndPort.fromString("localhost")), IllegalArgumentException.class); // no port
        assertThrows(base.withAddress(HostAndPort.fromString(":8080")), IllegalArgumentException.class);  // no hostname
        assertThrows(base.withAddress(null), IllegalArgumentException.class);
    }

    @Test
    public void testPayloadSize() {
        ServiceInstanceBuilder base = new ServiceInstanceBuilder();

        assertThrows(base.withPayload(string(MAX_PAYLOAD_SIZE_IN_CHARACTERS + 1)), IllegalArgumentException.class);
        base.withPayload(string(MAX_PAYLOAD_SIZE_IN_CHARACTERS)).build(); // small enough, doesn't throw
        base.withPayload("").build(); // doesn't throw
    }

    @Test
    public void testToJson() throws Exception {
        ServiceInstance instance = new ServiceInstance("FooService", SERVER);
        assertJson(instance.toJson(), instance);
    }

    @Test
    public void testToJsonWithPayload() throws Exception {
        ServiceInstance instance = new ServiceInstance("FooService", SERVER, "payload");
        assertJson(instance.toJson(), instance);
    }

    @Test
    public void testToJsonWithEmptyPayload() throws Exception {
        ServiceInstance instance = new ServiceInstance("FooService", SERVER, "");
        assertJson(instance.toJson(), instance);
    }

    @Test
    public void testFromJson() throws Exception {
        ServiceInstance instance = new ServiceInstance("FooService", SERVER);
        assertEquals(instance, ServiceInstance.fromJson(instance.toJson()));
    }

    @Test
    public void testFromJsonWithPayload() throws Exception {
        ServiceInstance instance = new ServiceInstance("FooService", SERVER, "payload");
        assertEquals(instance, ServiceInstance.fromJson(instance.toJson()));
    }

    @Test
    public void testFromJsonWithEmptyPayload() throws Exception {
        ServiceInstance instance = new ServiceInstance("FooService", SERVER, "");
        assertEquals(instance, ServiceInstance.fromJson(instance.toJson()));
    }

    @Test(expected = RuntimeException.class)
    public void testFromJsonWithMalformedJson() throws Exception {
        ServiceInstance.fromJson("{");
    }

    @Test(expected = NullPointerException.class)
    public void testFromJsonWithMissingName() throws Exception {
        ServiceInstance.fromJson(buildJson("name"));
    }

    @Test(expected = NullPointerException.class)
    public void testFromJsonWithMissingHost() throws Exception {
        ServiceInstance.fromJson(buildJson("host"));
    }

    @Test(expected = NullPointerException.class)
    public void testFromJsonWithMissingPort() throws Exception {
        ServiceInstance.fromJson(buildJson("port"));
    }

    @Test(expected = NullPointerException.class)
    public void testFromJsonWithMissingRegistrationTime() throws Exception {
        ServiceInstance.fromJson(buildJson("registration-time"));
    }

    @Test(expected = NullPointerException.class)
    public void testFromJsonWithMissingPayload() throws Exception {
        ServiceInstance.fromJson(buildJson("payload"));
    }

    private void assertThrows(ServiceInstanceBuilder builder, Class<? extends Throwable> cls) {
        try {
            builder.build();
            fail();
        } catch (Throwable t) {
            if (!cls.isInstance(t)) fail();
        }
    }

    private void assertJson(String json, ServiceInstance instance) throws Exception {
        JsonNode root = new ObjectMapper().readTree(json);

        assertEquals(instance.getServiceName(), root.get("name").textValue());
        assertEquals(instance.getHostname(), root.get("host").textValue());
        assertEquals(instance.getPort(), root.get("port").intValue());
        assertEquals(instance.getRegistrationTime(), ISO8601.parseDateTime(root.get("registration-time").textValue()));

        if (instance.getPayload() != null) {
            assertEquals(instance.getPayload(), root.get("payload").textValue());
        } else {
            assertNull(root.get("payload").textValue());
        }
    }

    private String string(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append('x');
        }
        return sb.toString();
    }

    private String buildJson(String without) throws IOException {
        StringWriter writer = new StringWriter();

        JsonGenerator generator = new JsonFactory().createJsonGenerator(writer);
        generator.writeStartObject();
        if (!"registration-time".equals(without)) {
            generator.writeStringField("registration-time", ISO8601.print(DateTime.now()));
        }

        if (!"name".equals(without)) {
            generator.writeStringField("name", "serviceName");
        }

        if (!"host".equals(without)) {
            generator.writeStringField("host", "server");
        }

        if (!"port".equals(without)) {
            generator.writeNumberField("port", 8080);
        }

        if (!"payload".equals(without)) {
            generator.writeStringField("payload", "payload");
        }
        generator.writeEndObject();
        generator.close();

        return writer.toString();
    }

    private static final class ServiceInstanceBuilder {
        private final String _serviceName;
        private final HostAndPort _address;
        private final String _payload;

        public ServiceInstanceBuilder() {
            this("Foo", HostAndPort.fromParts("localhost", 8080), null);
        }

        public ServiceInstanceBuilder(String serviceName, HostAndPort address, String payload) {
            _serviceName = serviceName;
            _address = address;
            _payload = payload;
        }

        public ServiceInstanceBuilder withName(String serviceName) {
            return new ServiceInstanceBuilder(serviceName, _address, _payload);
        }

        public ServiceInstanceBuilder withAddress(HostAndPort address) {
            return new ServiceInstanceBuilder(_serviceName, address, _payload);
        }

        public ServiceInstanceBuilder withPayload(String payload) {
            return new ServiceInstanceBuilder(_serviceName, _address, payload);
        }

        public ServiceInstance build() {
            return new ServiceInstance(_serviceName, _address, _payload);
        }
    }
}
