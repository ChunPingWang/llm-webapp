package com.example.llmagent.acceptance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.example.llmagent.adapter.out.docx.PoiDocumentTextExtractor;
import com.example.llmagent.adapter.out.docx.PoiDocxRenderer;
import com.example.llmagent.adapter.out.persistence.InMemoryAgentProfileStore;
import com.example.llmagent.adapter.out.persistence.InMemoryArtifactStore;
import com.example.llmagent.adapter.out.persistence.InMemoryAuditLogStore;
import com.example.llmagent.adapter.out.persistence.InMemoryConversationStore;
import com.example.llmagent.adapter.out.persistence.InMemoryFileMetadataStore;
import com.example.llmagent.application.AgentProfileService;
import com.example.llmagent.application.ArtifactService;
import com.example.llmagent.application.AttachmentTextService;
import com.example.llmagent.application.port.out.FileStorage;
import com.example.llmagent.domain.chat.Role;
import com.example.llmagent.domain.file.StoredFile;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** WP8-T1:平台驗收 steps(zh-TW feature + 英文 annotation)。以 fake provider 確保決定性。 */
public class PlatformAcceptanceSteps {

    private String cannedReply;
    private ChatService chatService;
    private ArtifactService artifactService;
    private AgentProfileService profileService;
    private com.example.llmagent.domain.agent.AgentProfile seededProfile;
    private String renderedPrompt;
    private InMemoryConversationStore conversationStore;
    private List<StreamEvent> events;
    private String conversationId;
    private byte[] docxBytes;
    private byte[] templateBytes;
    private String attachmentFileId;
    private AttachmentTextService attachmentService;
    private InMemoryFileMetadataStore fileMetadataStore;
    private final Map<String, byte[]> blobStore = new HashMap<>();

    @Before
    public void setup() {
        cannedReply = "";
        InMemoryArtifactStore artifactStore = new InMemoryArtifactStore();
        artifactService = new ArtifactService(artifactStore);
        conversationStore = new InMemoryConversationStore();
        profileService = new AgentProfileService(new InMemoryAgentProfileStore(), null);
        chatService = new ChatService(
                call -> Flux.just(ChatChunk.text(cannedReply), ChatChunk.finalUsage(new Usage(10, 5))),
                conversationStore,
                new RuntimeSettingsService(new ChatProperties("test-model", "sys"), "http://x", "k"),
                profileService,
                artifactService,
                new InMemoryAuditLogStore(),
                io.micrometer.observation.ObservationRegistry.create());

        blobStore.clear();
        fileMetadataStore = new InMemoryFileMetadataStore();
        FileStorage fakeStorage = new FileStorage() {
            @Override
            public void put(String key, byte[] content, String contentType) {
                blobStore.put(key, content);
            }

            @Override
            public byte[] get(String key) {
                return blobStore.get(key);
            }

            @Override
            public String presignedGetUrl(String key, int expirySeconds) {
                return "http://test/" + key;
            }
        };
        attachmentService = new AttachmentTextService(fileMetadataStore, fakeStorage,
                new PoiDocumentTextExtractor());
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

    @And("使用者清除對話與產出物")
    public void userClearsConversation() {
        chatService.deleteConversation(conversationId);
    }

    @Then("該對話應已不存在")
    public void conversationGone() {
        assertTrue(conversationStore.findById(conversationId).isEmpty(), "對話應已刪除");
    }

    @And("該對話應沒有任何 {string} 產出物")
    public void noArtifactsLeft(String type) {
        assertEquals(0, artifactService.versions(conversationId,
                com.example.llmagent.domain.artifact.Artifact.ArtifactType.valueOf(type)).size());
    }

    @When("系統種子化內建 Agent Profile")
    public void seedBuiltInProfiles() throws Exception {
        new com.example.llmagent.application.AgentProfileSeeder()
                .seedAgentProfiles(profileService, new PoiDocumentTextExtractor())
                .run(null);
    }

    @Then("應存在名為 {string} 的 Agent Profile")
    public void profileExists(String name) {
        seededProfile = profileService.listLatest().stream()
                .filter(p -> p.name().equals(name))
                .findFirst().orElse(null);
        assertNotNull(seededProfile, "缺少內建 Profile: " + name);
    }

    @And("該 Profile 的 system prompt 應包含 {string}")
    public void profilePromptContains(String expected) {
        assertTrue(seededProfile.systemPrompt().contains(expected),
                "system prompt 缺少: " + expected);
    }

    @When("以變數 project_name 為 {string} 渲染範本 {string}")
    public void renderPromptTemplate(String value, String template) {
        renderedPrompt = com.example.llmagent.domain.agent.PromptTemplate
                .render(template, Map.of("project_name", value));
    }

    @Then("渲染結果應為 {string}")
    public void renderedPromptEquals(String expected) {
        assertEquals(expected, renderedPrompt);
    }

    @Given("已上傳附件 {string} 內容為 {string}")
    public void attachmentUploaded(String filename, String text) {
        attachmentFileId = UUID.randomUUID().toString();
        String key = "uploads/" + attachmentFileId + "/" + filename;
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        blobStore.put(key, bytes);
        fileMetadataStore.save(new StoredFile(attachmentFileId, filename, "text/plain",
                bytes.length, key, Instant.now()));
    }

    @When("使用者送出訊息 {string} 並附上該附件")
    public void userSendsWithAttachment(String content) {
        var conv = chatService.createConversation("t", null, null, null, null, null);
        conversationId = conv.id();
        String augmented = attachmentService.augment(content, List.of(attachmentFileId));
        chatService.addUserMessage(conversationId, augmented);
    }

    @Then("送給模型的使用者訊息應包含 {string}")
    public void userMessageContains(String expected) {
        String content = conversationStore.findById(conversationId).orElseThrow()
                .messages().stream()
                .filter(m -> m.role() == Role.USER)
                .findFirst().orElseThrow()
                .content();
        assertTrue(content.contains(expected), "使用者訊息缺少: " + expected);
    }

    @Given("一份含 {string} 與 {string} 佔位的 Word 範本")
    public void wordTemplate(String p1, String p2) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText(p1);
            doc.createParagraph().createRun().setText(p2);
            doc.write(out);
            templateBytes = out.toByteArray();
        }
    }

    @When("以標題 {string} 及該範本將 Markdown {string} 套版轉為 Word")
    public void renderDocxWithTemplate(String title, String markdown) {
        docxBytes = new PoiDocxRenderer()
                .renderWithTemplate(title, markdown.replace("\\n", "\n"), templateBytes);
    }

    @And("產生的 docx 不應包含文字 {string}")
    public void docxNotContains(String text) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            String all = String.join("\n", doc.getParagraphs().stream().map(p -> p.getText()).toList());
            assertFalse(all.contains(text), "docx 不應包含: " + text);
        }
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
