package br.com.bilca.event.domain;

import java.time.OffsetDateTime;

public record Participant(Long id, Long eventId, String name, String email, String phone, String stack,
                          OffsetDateTime materialsSentAt) {
}
