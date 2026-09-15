package br.com.bilca.event.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "PARTICIPANTS",
        uniqueConstraints = @UniqueConstraint(name = "UK_PARTICIPANT_EVENT_EMAIL", columnNames = {"EVENT_ID", "EMAIL"}))
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "participants_seq")
    @SequenceGenerator(name = "participants_seq", sequenceName = "PARTICIPANTS_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EVENT_ID", nullable = false)
    private Event event;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "TECH_STACK", nullable = false, length = 150)
    private String stack;

    protected Participant() {
    }

    public Participant(Event event, String name, String email, String phone, String stack) {
        this.event = event;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.stack = stack;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getStack() {
        return stack;
    }
}
