package br.com.bilca.event.web;

import br.com.bilca.event.domain.Event;
import br.com.bilca.event.domain.EventDocument;
import br.com.bilca.event.domain.Participant;
import br.com.bilca.event.repository.EventDocumentRepository;
import br.com.bilca.event.repository.EventRepository;
import br.com.bilca.event.repository.ParticipantRepository;
import br.com.bilca.event.service.MailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EventDocumentRepository eventDocumentRepository;
    private final MailService mailService;
    private final AdminAuth adminAuth;

    public EventController(EventRepository eventRepository,
                           ParticipantRepository participantRepository,
                           EventDocumentRepository eventDocumentRepository,
                           MailService mailService,
                           AdminAuth adminAuth) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.eventDocumentRepository = eventDocumentRepository;
        this.mailService = mailService;
        this.adminAuth = adminAuth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                @Valid @RequestBody CreateEventRequest request) {
        adminAuth.require(token);
        Event event = eventRepository.save(request.name(), request.date(), request.host(), request.location(),
                request.capacity());
        return EventResponse.from(event, 0, 0);
    }

    @GetMapping
    public List<EventResponse> list(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        adminAuth.require(token);
        return eventRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{code}")
    public EventResponse findByCode(@PathVariable String code) {
        return toResponse(findEventOrThrow(code));
    }

    @PutMapping("/{code}")
    public EventResponse update(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                @PathVariable String code,
                                @Valid @RequestBody CreateEventRequest request) {
        adminAuth.require(token);
        findEventOrThrow(code);
        eventRepository.update(code, request.name(), request.date(), request.host(), request.location(),
                request.capacity());
        return toResponse(eventRepository.findByPublicCode(code).orElseThrow());
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                       @PathVariable String code) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        int participantCount = participantRepository.countByEventId(event.id());
        if (participantCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir: " + participantCount + " participante(s) já inscrito(s)");
        }
        eventRepository.deleteByPublicCode(code);
    }

    @PostMapping("/{code}/send-materials")
    public SendMaterialsResponse sendMaterialsToPending(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable String code) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        List<EventDocument> documents = eventDocumentRepository.findMetadataByEventId(event.id());
        if (documents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum documento cadastrado para este evento");
        }
        int sent = 0;
        int failed = 0;
        for (Participant participant : participantRepository.findByEventId(event.id())) {
            if (participant.materialsSentAt() != null) {
                continue;
            }
            try {
                mailService.sendMaterials(event, participant, documents);
                participantRepository.markMaterialsSent(participant.id(), event.id(), true);
                sent++;
            } catch (Exception e) {
                log.warn("Falha ao enviar materiais para {}", participant.email(), e);
                failed++;
            }
        }
        return new SendMaterialsResponse(sent, failed);
    }

    private Event findEventOrThrow(String code) {
        return eventRepository.findByPublicCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.from(event, participantRepository.countByEventId(event.id()),
                eventDocumentRepository.countByEventId(event.id()));
    }

    public record CreateEventRequest(
            @NotBlank(message = "Nome é obrigatório") String name,
            @NotNull(message = "Data é obrigatória") LocalDate date,
            @NotBlank(message = "Host é obrigatório") String host,
            @NotBlank(message = "Local é obrigatório") String location,
            @NotNull(message = "Capacidade é obrigatória")
            @Positive(message = "Capacidade deve ser maior que zero") Integer capacity) {
    }

    public record EventResponse(Long id, String code, String name, LocalDate date, String host, String location,
                                Integer capacity, int participantCount, int documentCount) {
        public static EventResponse from(Event event, int participantCount, int documentCount) {
            return new EventResponse(event.id(), event.publicCode(), event.name(), event.date(), event.host(),
                    event.location(), event.capacity(), participantCount, documentCount);
        }
    }

    public record SendMaterialsResponse(int sent, int failed) {
    }
}
