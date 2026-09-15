package br.com.bilca.event.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "EVENTS", indexes = @Index(name = "IDX_EVENTS_PUBLIC_CODE", columnList = "PUBLIC_CODE", unique = true))
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "events_seq")
    @SequenceGenerator(name = "events_seq", sequenceName = "EVENTS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "PUBLIC_CODE", nullable = false, unique = true, length = 36)
    private String publicCode = UUID.randomUUID().toString();

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "EVENT_DATE", nullable = false)
    private LocalDate date;

    @Column(name = "EVENT_HOST", nullable = false, length = 150)
    private String host;

    protected Event() {
    }

    public Event(String name, LocalDate date, String host) {
        this.name = name;
        this.date = date;
        this.host = host;
    }

    public Long getId() {
        return id;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getHost() {
        return host;
    }
}
