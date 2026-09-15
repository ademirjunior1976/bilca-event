package br.com.bilca.event.web;

import br.com.bilca.event.domain.Event;
import br.com.bilca.event.repository.EventRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final String adminApiToken;

    public EventController(EventRepository eventRepository,
                           @Value("${bilca.admin-api-token:}") String adminApiToken) {
        this.eventRepository = eventRepository;
        this.adminApiToken = adminApiToken;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                @Valid @RequestBody CreateEventRequest request) {
        if (adminApiToken.isBlank() || !adminApiToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token administrativo inválido");
        }
        Event event = eventRepository.save(request.name(), request.date(), request.host());
        return EventResponse.from(event);
    }

    @GetMapping("/{code}")
    public EventResponse findByCode(@PathVariable String code) {
        return eventRepository.findByPublicCode(code)
                .map(EventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
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
