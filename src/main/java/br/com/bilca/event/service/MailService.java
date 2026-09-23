package br.com.bilca.event.service;

import br.com.bilca.event.domain.Event;
import br.com.bilca.event.domain.EventDocument;
import br.com.bilca.event.domain.Participant;
import br.com.bilca.event.repository.EventDocumentRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class MailService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.of("pt", "BR"));

    private final JavaMailSender mailSender;
    private final EventDocumentRepository eventDocumentRepository;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender, EventDocumentRepository eventDocumentRepository,
                       @Value("${bilca.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.eventDocumentRepository = eventDocumentRepository;
        this.fromAddress = fromAddress;
    }

    /**
     * Envia todos os documentos do evento anexados num único e-mail.
     * Retorna false (sem enviar nada) quando o evento não tem documentos.
     */
    public boolean sendMaterials(Event event, Participant participant, List<EventDocument> documents) {
        if (documents.isEmpty()) {
            return false;
        }
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(participant.email());
            if (!fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setSubject("Materiais do evento " + event.name());
            helper.setText(buildBody(event, participant), false);
            for (EventDocument document : documents) {
                byte[] data = eventDocumentRepository.findDataByIdAndEventId(document.id(), event.id())
                        .orElseThrow(() -> new IllegalStateException("Documento não encontrado: " + document.id()));
                helper.addAttachment(document.fileName(), new ByteArrayResource(data), document.contentType());
            }
        } catch (Exception e) {
            throw new MailSendFailedException("Falha ao montar e-mail para " + participant.email(), e);
        }
        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new MailSendFailedException("Falha ao enviar e-mail para " + participant.email(), e);
        }
        return true;
    }

    private String buildBody(Event event, Participant participant) {
        return "Olá, " + participant.name() + "!\n\n"
                + "Segue em anexo o material do evento \"" + event.name() + "\", realizado em "
                + event.date().format(DATE_FORMAT) + " em " + event.location() + ".\n\n"
                + "Att,\nBilca Systems";
    }

    public static class MailSendFailedException extends RuntimeException {
        public MailSendFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
