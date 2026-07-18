package com.example.llmagent.acceptance;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.example.llmagent.adapter.out.docx.PoiDocxRenderer;
import com.example.llmagent.adapter.out.persistence.InMemoryAgentProfileStore;
import com.example.llmagent.adapter.out.persistence.InMemoryArtifactStore;
import com.example.llmagent.adapter.out.persistence.InMemoryAuditLogStore;
import com.example.llmagent.adapter.out.persistence.InMemoryConversationStore;
import com.example.llmagent.application.AgentProfileService;
import com.example.llmagent.application.ArtifactService;
import com.example.llmagent.application.ChatProperties;
import com.example.llmagent.application.ChatService;
import com.example.llmagent.application.RuntimeSettingsService;
import com.example.llmagent.application.event.StreamEvent;
import com.example.llmagent.domain.chat.ChatChunk;
import com.example.llmagent.domain.chat.Usage;
import com.example.llmagent.domain.sse.SseEventType;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** WP8-T1:平台驗收 steps(zh-TW feature + 英文 annotation)。以 fake provider 確保決定性。 */
public class PlatformAcceptanceSteps {

    private String cannedReply;
    private ChatService chatService;
    private ArtifactService artifactService;
    private List<StreamEvent> events;
    private String conversationId;
    private byte[] docxBytes;

    @Before
    public void setup() {
        cannedReply = "";
        InMemoryArtifactStore artifactStore = new InMemoryArtifactStore();
        artifactService = new ArtifactService(artifactStore);
        chatService = new ChatService(
                call -> Flux.just(ChatChunk.text(cannedReply), ChatChunk.finalUsage(new Usage(10, 5))),
                new InMemoryConversationStore(),
                new RuntimeSettingsService(new ChatProperties("test-model", "sys"), "http://x", "k"),
                new AgentProfileService(new InMemoryAgentProfileStore(), null),
                artifactService,
                new InMemoryAuditLogStore(),
                io.micrometer.observation.ObservationRegistry.create());
    }

    @Given("模型將回覆 {string}")
    public void modelWillReply(String reply) {
        cannedReply = reply.replace("\\n", "\n");
    }

    @When("使用者送出訊息 {string}")
    public void userSends(String content) {
        var conv = chatService.createConversation("t", null, null, null, null, null);
        conversationId = conv.id();
        String messageId = chatService.addUserMessage(conversationId, content);
        events = chatService.streamAssistant(messageId).collectList().block();
        assertNotNull(events);
    }

    @Then("事件序列應依序包含 {string} 再 {string} 再 {string}")
    public void eventOrder(String a, String b, String c) {
        int ia = firstIndexOf(a);
        int ib = firstIndexOf(b);
        int ic = firstIndexOf(c);
        assertTrue(ia >= 0 && ib > ia && ic > ib,
                "順序錯誤: %s@%d %s@%d %s@%d".formatted(a, ia, b, ib, c, ic));
    }

    private int firstIndexOf(String wireName) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).type().wireName().equals(wireName)) {
                return i;
            }
        }
        return -1;
    }

    @And("思考事件內容合併後應為 {string}")
    public void thinkingJoined(String expected) {
        assertEquals(expected, joined(SseEventType.THINKING));
    }

    @And("內容事件合併後應為 {string}")
    public void contentJoined(String expected) {
        assertEquals(expected, joined(SseEventType.CONTENT));
    }

    private String joined(SseEventType type) {
        StringBuilder sb = new StringBuilder();
        for (StreamEvent e : events) {
            if (e.type() == type) {
                if (e.payload() instanceof StreamEvent.ThinkingDelta t) {
                    sb.append(t.delta());
                } else if (e.payload() instanceof StreamEvent.ContentDelta cd) {
                    sb.append(cd.delta());
                }
            }
        }
        return sb.toString();
    }

    @Then("該回覆應產生 {int} 個 {string} 產出物且版本為 {int}")
    public void artifactCreated(int count, String type, int version) {
        var artifacts = artifactService.versions(conversationId,
                com.example.llmagent.domain.artifact.Artifact.ArtifactType.valueOf(type));
        assertEquals(count, artifacts.size());
        assertEquals(version, artifacts.get(artifacts.size() - 1).version());
    }

    @When("以標題 {string} 將 Markdown {string} 轉為 Word")
    public void renderDocx(String title, String markdown) {
        docxBytes = new PoiDocxRenderer().render(title, markdown.replace("\\n", "\n"));
    }

    @Then("產生的 docx 應可解析且包含文字 {string}")
    public void docxContains(String text) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            String all = String.join("\n", doc.getParagraphs().stream().map(p -> p.getText()).toList());
            assertTrue(all.contains(text), "docx 缺少文字: " + text);
        }
    }

    @And("產生的 docx 應包含 {int} 個表格")
    public void docxTables(int count) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            assertEquals(count, doc.getTables().size());
        }
    }
}
