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

@RestController
@RequestMapping("/api/events/{code}/participants")
public class ParticipantController {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    public ParticipantController(EventRepository eventRepository, ParticipantRepository participantRepository) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantResponse create(@PathVariable String code,
                                      @Valid @RequestBody CreateParticipantRequest request) {
        Event event = eventRepository.findByPublicCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));

        if (participantRepository.existsByEventIdAndEmailIgnoreCase(event.getId(), request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está inscrito neste evento");
        }

        Participant participant = participantRepository.save(
                new Participant(event, request.name(), request.email(), request.phone(), request.stack()));
        return ParticipantResponse.from(participant);
    }

    public record CreateParticipantRequest(
            @NotBlank(message = "Nome é obrigatório") String name,
            @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
            @NotBlank(message = "Telefone é obrigatório") String phone,
            @NotBlank(message = "Stack é obrigatória") String stack) {
    }

    public record ParticipantResponse(Long id, String name, String email, String phone, String stack) {
        public static ParticipantResponse from(Participant participant) {
            return new ParticipantResponse(participant.getId(), participant.getName(), participant.getEmail(),
                    participant.getPhone(), participant.getStack());
        }
    }
}
