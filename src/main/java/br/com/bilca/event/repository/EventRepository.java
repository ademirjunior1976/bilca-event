package br.com.bilca.event.repository;

import br.com.bilca.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByPublicCode(String publicCode);
}
