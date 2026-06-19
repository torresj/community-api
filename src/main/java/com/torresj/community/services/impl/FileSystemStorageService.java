package com.torresj.community.services.impl;

import com.torresj.community.exceptions.StorageException;
import com.torresj.community.services.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Stores files on the local filesystem (a Kubernetes persistent volume in prod). Only the
 * relative file name is persisted in the database; the content lives on the volume.
 */
@Service
@Slf4j
public class FileSystemStorageService implements FileStorageService {

    private final Path root;

    public FileSystemStorageService(@Value("${storage.pdf.path}") String path) {
        this.root = Paths.get(path).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] content, String originalFilename) {
        try {
            Files.createDirectories(root);
            String name = UUID.randomUUID() + extensionOf(originalFilename);
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) {
                throw new StorageException("Resolved path is outside the storage root");
            }
            Files.write(target, content);
            log.info("Stored file {}", name);
            return name;
        } catch (IOException e) {
            throw new StorageException("Could not store file", e);
        }
    }

    @Override
    public Resource load(String relativePath) {
        try {
            Path file = root.resolve(relativePath).normalize();
            if (!file.startsWith(root)) {
                throw new StorageException("Requested path is outside the storage root");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new StorageException("Could not read file: " + relativePath);
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file: " + relativePath, e);
        }
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return ".pdf";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : ".pdf";
    }
}
