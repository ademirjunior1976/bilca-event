package br.com.bilca.event.web;

import br.com.bilca.event.domain.Event;
import br.com.bilca.event.domain.EventDocument;
import br.com.bilca.event.domain.Participant;
import br.com.bilca.event.repository.EventDocumentRepository;
import br.com.bilca.event.repository.EventRepository;
import br.com.bilca.event.repository.ParticipantRepository;
import br.com.bilca.event.service.MailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events/{code}/participants")
public class ParticipantController {

    private static final Logger log = LoggerFactory.getLogger(ParticipantController.class);

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EventDocumentRepository eventDocumentRepository;
    private final MailService mailService;
    private final AdminAuth adminAuth;

    public ParticipantController(EventRepository eventRepository, ParticipantRepository participantRepository,
                                 EventDocumentRepository eventDocumentRepository, MailService mailService,
                                 AdminAuth adminAuth) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.eventDocumentRepository = eventDocumentRepository;
        this.mailService = mailService;
        this.adminAuth = adminAuth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantResponse create(@PathVariable String code,
                                      @Valid @RequestBody CreateParticipantRequest request) {
        Event event = findEventOrThrow(code);

        if (participantRepository.existsByEventIdAndEmailIgnoreCase(event.id(), request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está inscrito neste evento");
        }

        Participant participant = participantRepository.save(
                event.id(), request.name(), request.email(), request.phone(), request.stack());

        // Falha de e-mail não pode impedir a inscrição: só registra no log.
        try {
            List<EventDocument> documents = eventDocumentRepository.findMetadataByEventId(event.id());
            if (mailService.sendMaterials(event, participant, documents)) {
                participantRepository.markMaterialsSent(participant.id(), event.id(), true);
                participant = findParticipantOrThrow(participant.id(), event.id());
            }
        } catch (Exception e) {
            log.warn("Não foi possível enviar os materiais automaticamente para {}: {}",
                    participant.email(), e.getMessage());
        }
        return ParticipantResponse.from(participant);
    }

    @GetMapping
    public List<ParticipantResponse> list(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                          @PathVariable String code) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        return participantRepository.findByEventId(event.id()).stream().map(ParticipantResponse::from).toList();
    }

    @PutMapping("/{id}")
    public ParticipantResponse update(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                      @PathVariable String code, @PathVariable Long id,
                                      @Valid @RequestBody CreateParticipantRequest request) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        if (participantRepository.existsByEventIdAndEmailIgnoreCaseExcludingId(event.id(), request.email(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está inscrito neste evento");
        }
        int updated = participantRepository.update(
                id, event.id(), request.name(), request.email(), request.phone(), request.stack());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado");
        }
        return ParticipantResponse.from(findParticipantOrThrow(id, event.id()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                       @PathVariable String code, @PathVariable Long id) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        int deleted = participantRepository.deleteByIdAndEventId(id, event.id());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado");
        }
    }

    @PostMapping("/{id}/send-materials")
    public ParticipantResponse sendMaterials(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                             @PathVariable String code, @PathVariable Long id) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        Participant participant = findParticipantOrThrow(id, event.id());
        List<EventDocument> documents = eventDocumentRepository.findMetadataByEventId(event.id());
        if (documents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum documento cadastrado para este evento");
        }
        try {
            mailService.sendMaterials(event, participant, documents);
        } catch (Exception e) {
            log.warn("Falha ao enviar materiais para {}: {}", participant.email(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível enviar o e-mail");
        }
        participantRepository.markMaterialsSent(id, event.id(), true);
        return ParticipantResponse.from(findParticipantOrThrow(id, event.id()));
    }

    @PatchMapping("/{id}/materials-sent")
    public ParticipantResponse setMaterialsSent(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                @PathVariable String code, @PathVariable Long id,
                                                @RequestBody MaterialsSentRequest request) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        int updated = participantRepository.markMaterialsSent(id, event.id(), request.sent());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado");
        }
        return ParticipantResponse.from(findParticipantOrThrow(id, event.id()));
    }

    private Event findEventOrThrow(String code) {
        return eventRepository.findByPublicCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
    }

    private Participant findParticipantOrThrow(Long id, Long eventId) {
        return participantRepository.findByIdAndEventId(id, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado"));
    }

    public record CreateParticipantRequest(
            @NotBlank(message = "Nome é obrigatório") String name,
            @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
            @NotBlank(message = "Telefone é obrigatório") String phone,
            @NotBlank(message = "Stack é obrigatória") String stack) {
    }

    public record MaterialsSentRequest(boolean sent) {
    }

    public record ParticipantResponse(Long id, String name, String email, String phone, String stack,
                                      OffsetDateTime materialsSentAt) {
        public static ParticipantResponse from(Participant participant) {
            return new ParticipantResponse(participant.id(), participant.name(), participant.email(),
                    participant.phone(), participant.stack(), participant.materialsSentAt());
        }
    }
}
