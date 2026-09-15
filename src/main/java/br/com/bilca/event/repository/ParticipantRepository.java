package br.com.bilca.event.repository;

import br.com.bilca.event.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    boolean existsByEventIdAndEmailIgnoreCase(Long eventId, String email);
}
