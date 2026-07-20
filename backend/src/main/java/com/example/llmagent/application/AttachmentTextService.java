package com.example.llmagent.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.llmagent.application.port.out.DocumentTextExtractor;
import com.example.llmagent.application.port.out.FileMetadataStore;
import com.example.llmagent.application.port.out.FileStorage;
import com.example.llmagent.domain.file.StoredFile;

/**
 * 對話附件服務:將已上傳檔案(/api/files)的文字內容併入使用者訊息,
 * 供模型參考(如上傳需求 Word 轉 Gherkin)。
 */
@Service
public class AttachmentTextService {

    private final FileMetadataStore metadata;
    private final FileStorage storage;
    private final DocumentTextExtractor extractor;

    public AttachmentTextService(FileMetadataStore metadata, FileStorage storage,
                                 DocumentTextExtractor extractor) {
        this.metadata = metadata;
        this.storage = storage;
        this.extractor = extractor;
    }

    /** 將附件文字附加於訊息之後;無附件時原樣返回。 */
    public String augment(String content, List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder(content);
        for (String fileId : fileIds) {
            StoredFile f = metadata.findById(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("file not found: " + fileId));
            String text = extractor.extractText(f.filename(), storage.get(f.storageKey()));
            sb.append("\n\n---\n【附件:").append(f.filename()).append("】\n").append(text);
        }
        return sb.toString();
    }
}
