package br.com.bilca.event.domain;

import java.time.LocalDate;

public record Event(Long id, String publicCode, String name, LocalDate date, String host) {
}
