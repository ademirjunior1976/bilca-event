package br.com.bilca.event.domain;

public record Participant(Long id, Long eventId, String name, String email, String phone, String stack) {
}
