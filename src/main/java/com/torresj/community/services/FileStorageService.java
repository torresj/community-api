package com.torresj.community.services;

import org.springframework.core.io.Resource;

public interface FileStorageService {
    /** Persist the given content and return the relative path/reference to store in the DB. */
    String store(byte[] content, String originalFilename);

    /** Load a previously stored file by its relative path/reference. */
    Resource load(String relativePath);
}
