package br.com.bilca.event.web;

import br.com.bilca.event.domain.Event;
import br.com.bilca.event.repository.EventRepository;
import br.com.bilca.event.repository.ParticipantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final String adminApiToken;

    public EventController(EventRepository eventRepository,
                           ParticipantRepository participantRepository,
                           @Value("${bilca.admin-api-token:}") String adminApiToken) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.adminApiToken = adminApiToken;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                @Valid @RequestBody CreateEventRequest request) {
        requireAdmin(token);
        Event event = eventRepository.save(request.name(), request.date(), request.host());
        return EventResponse.from(event);
    }

    @GetMapping
    public List<EventResponse> list(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        requireAdmin(token);
        return eventRepository.findAll().stream().map(EventResponse::from).toList();
    }

    private void requireAdmin(String token) {
        if (adminApiToken.isBlank() || !adminApiToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token administrativo inválido");
        }
    }

    @GetMapping("/{code}")
    public EventResponse findByCode(@PathVariable String code) {
        return eventRepository.findByPublicCode(code)
                .map(EventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
    }

    @PutMapping("/{code}")
    public EventResponse update(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                @PathVariable String code,
                                @Valid @RequestBody CreateEventRequest request) {
        requireAdmin(token);
        eventRepository.findByPublicCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
        eventRepository.update(code, request.name(), request.date(), request.host());
        return eventRepository.findByPublicCode(code).map(EventResponse::from).orElseThrow();
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                       @PathVariable String code) {
        requireAdmin(token);
        Event event = eventRepository.findByPublicCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
        int participantCount = participantRepository.countByEventId(event.id());
        if (participantCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir: " + participantCount + " participante(s) já inscrito(s)");
        }
        eventRepository.deleteByPublicCode(code);
    }

    public record CreateEventRequest(
            @NotBlank(message = "Nome é obrigatório") String name,
            @NotNull(message = "Data é obrigatória") LocalDate date,
            @NotBlank(message = "Host é obrigatório") String host) {
    }

    public record EventResponse(Long id, String code, String name, LocalDate date, String host) {
        public static EventResponse from(Event event) {
            return new EventResponse(event.id(), event.publicCode(), event.name(), event.date(), event.host());
        }
    }
}
