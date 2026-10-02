package com.jobportal.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Metadata for an uploaded resume. Phase 1 stores only a path; file storage comes later.
 */
public record Resume(String fileName, String storagePath, LocalDateTime uploadedAt) {
    public Resume {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(storagePath, "storagePath");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
    }
}
