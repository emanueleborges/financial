package com.financialhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos ITs: Postgres + Kafka compartilhados na JVM (não param entre classes).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("it")
public abstract class AbstractIntegrationTest {

    private static final boolean DOCKER_AVAILABLE;
    private static final PostgreSQLContainer<?> POSTGRES;
    private static final KafkaContainer KAFKA;

    static {
        boolean dockerOk = false;
        PostgreSQLContainer<?> postgres = null;
        KafkaContainer kafka = null;
        try {
            DockerClientFactory.instance().client();
            dockerOk = true;
            postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("financialhub")
                    .withUsername("financial")
                    .withPassword("financial123")
                    .withReuse(false);
            kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));
            postgres.start();
            kafka.start();
        } catch (Throwable ex) {
            dockerOk = false;
            if (postgres != null) {
                postgres.stop();
            }
            if (kafka != null) {
                kafka.stop();
            }
            postgres = null;
            kafka = null;
        }
        DOCKER_AVAILABLE = dockerOk;
        POSTGRES = postgres;
        KAFKA = kafka;
    }

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE,
                "Docker indisponível — ITs Testcontainers ignorados");
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (!DOCKER_AVAILABLE) {
            return;
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.aws.s3.endpoint", () -> "http://127.0.0.1:1");
    }

    protected static final String PAYER_DOC = "52998224725";
    protected static final String PAYEE_DOC = "39053344705";
    protected static final String PASSWORD = "senha123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE);
        jdbcTemplate.execute(
                "TRUNCATE TABLE transaction_audit, processed_events, transactions, users CASCADE");
    }

    protected void createUser(String name, String email, String document, BigDecimal balance)
            throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "email", email,
                                "document", document,
                                "password", PASSWORD,
                                "initialBalance", balance
                        ))))
                .andExpect(status().isCreated());
    }

    protected String login(String document) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "document", document,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    protected void seedPayerAndPayee() throws Exception {
        createUser("Alice Silva", "alice-" + UUID.randomUUID() + "@test.com",
                PAYER_DOC, new BigDecimal("2000.00"));
        createUser("Bob Santos", "bob-" + UUID.randomUUID() + "@test.com",
                PAYEE_DOC, new BigDecimal("500.00"));
    }
}
