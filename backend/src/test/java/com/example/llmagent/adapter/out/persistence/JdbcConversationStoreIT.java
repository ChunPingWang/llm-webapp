package com.example.llmagent.adapter.out.persistence;

import java.sql.Connection;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.example.llmagent.domain.chat.Conversation;
import com.example.llmagent.domain.chat.Message;
import com.example.llmagent.domain.chat.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * WP1-T3 驗收:Flyway V1 migration 於 PostgreSQL 通過;JdbcConversationStore 往返正確。
 *
 * <p>需本機 Postgres(docker compose -f docker/compose.yaml up -d postgres);
 * 未啟動時本測試自動 skip。使用獨立 schema {@code it_test},每次執行前 clean。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JdbcConversationStoreIT {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/llmagent?currentSchema=it_test";

    private JdbcConversationStore store;

    @BeforeAll
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(URL, "llmagent", "llmagent");
        boolean up;
        try (Connection ignored = ds.getConnection()) {
            up = true;
        } catch (Exception e) {
            up = false;
        }
        assumeTrue(up, "PostgreSQL 未啟動,跳過整合測試");

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .schemas("it_test")
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate(); // WP1-T3 驗收:V1 migration 通過

        store = new JdbcConversationStore(new JdbcTemplate(ds));
    }

    @Test
    void roundTripsConversationWithOrderedMessages() {
        String convId = UUID.randomUUID().toString();
        Conversation c = new Conversation(convId, "IT 測試", "sys", "claude-opus-4-8", 1.0, Instant.now());
        c.addMessage(Message.user(UUID.randomUUID().toString(), "第一句", Instant.now()));
        c.addMessage(Message.assistant(UUID.randomUUID().toString(), "claude-opus-4-8", "回覆一", Instant.now()));
        c.addMessage(Message.user(UUID.randomUUID().toString(), "第二句", Instant.now()));
        store.save(c);

        Optional<Conversation> loaded = store.findById(convId);
        assertThat(loaded).isPresent();
        Conversation l = loaded.get();
        assertThat(l.title()).isEqualTo("IT 測試");
        assertThat(l.systemPrompt()).isEqualTo("sys");
        assertThat(l.messages()).hasSize(3);
        assertThat(l.messages().get(0).content()).isEqualTo("第一句");
        assertThat(l.messages().get(1).role()).isEqualTo(Role.ASSISTANT);
        assertThat(l.messages().get(2).content()).isEqualTo("第二句");
    }

    @Test
    void findsConversationByMessageId() {
        String convId = UUID.randomUUID().toString();
        String msgId = UUID.randomUUID().toString();
        Conversation c = new Conversation(convId, "byMsg", null, "m", null, Instant.now());
        c.addMessage(Message.user(msgId, "hello", Instant.now()));
        store.save(c);

        assertThat(store.findByMessageId(msgId)).isPresent()
                .get().extracting(Conversation::id).isEqualTo(convId);
        assertThat(store.findByMessageId(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void appendOnlySaveIsIdempotentForExistingMessages() {
        String convId = UUID.randomUUID().toString();
        Conversation c = new Conversation(convId, "idem", null, "m", null, Instant.now());
        c.addMessage(Message.user(UUID.randomUUID().toString(), "once", Instant.now()));
        store.save(c);
        store.save(c); // 重複儲存不得重複插入

        assertThat(store.findById(convId).orElseThrow().messages()).hasSize(1);
    }
}
