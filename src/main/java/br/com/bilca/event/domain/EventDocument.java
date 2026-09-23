package br.com.bilca.event.domain;

import java.time.OffsetDateTime;

public record EventDocument(Long id, Long eventId, String fileName, String contentType, long fileSize,
                            OffsetDateTime uploadedAt) {
}
