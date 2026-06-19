package com.torresj.community.services;

import com.torresj.community.exceptions.StorageException;
import com.torresj.community.services.impl.FileSystemStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeThenLoadRoundTrips() throws Exception {
        var service = new FileSystemStorageService(tempDir.toString());
        byte[] content = "PDF-CONTENT".getBytes();

        String path = service.store(content, "acta.pdf");

        assertThat(path).endsWith(".pdf");
        var resource = service.load(path);
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    void storeDefaultsExtensionWhenMissing() {
        var service = new FileSystemStorageService(tempDir.toString());
        String path = service.store("x".getBytes(), null);
        assertThat(path).endsWith(".pdf");
    }

    @Test
    void loadMissingFileThrows() {
        var service = new FileSystemStorageService(tempDir.toString());
        assertThatThrownBy(() -> service.load("does-not-exist.pdf"))
                .isInstanceOf(StorageException.class);
    }
}
