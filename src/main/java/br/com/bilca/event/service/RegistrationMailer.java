package br.com.bilca.event.service;

import br.com.bilca.event.domain.Event;
import br.com.bilca.event.domain.EventDocument;
import br.com.bilca.event.domain.Participant;
import br.com.bilca.event.repository.EventDocumentRepository;
import br.com.bilca.event.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Envia os materiais logo após a inscrição, em segundo plano: o participante recebe a
 * confirmação na hora, sem esperar a conexão com o Gmail. Falhas só vão para o log.
 */
@Component
public class RegistrationMailer {

    private static final Logger log = LoggerFactory.getLogger(RegistrationMailer.class);

    private final EventDocumentRepository eventDocumentRepository;
    private final ParticipantRepository participantRepository;
    private final MailService mailService;

    public RegistrationMailer(EventDocumentRepository eventDocumentRepository,
                              ParticipantRepository participantRepository,
                              MailService mailService) {
        this.eventDocumentRepository = eventDocumentRepository;
        this.participantRepository = participantRepository;
        this.mailService = mailService;
    }

    @Async
    public void sendMaterialsAfterRegistration(Event event, Participant participant) {
        try {
            List<EventDocument> documents = eventDocumentRepository.findMetadataByEventId(event.id());
            if (mailService.sendMaterials(event, participant, mailService.loadAttachments(event, documents))) {
                participantRepository.markMaterialsSent(participant.id(), event.id(), true);
            }
        } catch (Exception e) {
            log.warn("Não foi possível enviar os materiais automaticamente para {}", participant.email(), e);
        }
    }
}
