package br.com.bilca.event.web;

import br.com.bilca.event.domain.Event;
import br.com.bilca.event.domain.Participant;
import br.com.bilca.event.repository.EventRepository;
import br.com.bilca.event.repository.ParticipantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/events/{code}/participants")
public class ParticipantController {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final AdminAuth adminAuth;

    public ParticipantController(EventRepository eventRepository, ParticipantRepository participantRepository,
                                 AdminAuth adminAuth) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
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
        return new ParticipantResponse(id, request.name(), request.email(), request.phone(), request.stack());
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

    private Event findEventOrThrow(String code) {
        return eventRepository.findByPublicCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
    }

    public record CreateParticipantRequest(
            @NotBlank(message = "Nome é obrigatório") String name,
            @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
            @NotBlank(message = "Telefone é obrigatório") String phone,
            @NotBlank(message = "Stack é obrigatória") String stack) {
    }

    public record ParticipantResponse(Long id, String name, String email, String phone, String stack) {
        public static ParticipantResponse from(Participant participant) {
            return new ParticipantResponse(participant.id(), participant.name(), participant.email(),
                    participant.phone(), participant.stack());
        }
    }
}
