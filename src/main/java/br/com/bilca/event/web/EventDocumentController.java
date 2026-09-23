package br.com.bilca.event.web;

import br.com.bilca.event.domain.Event;
import br.com.bilca.event.domain.EventDocument;
import br.com.bilca.event.repository.EventDocumentRepository;
import br.com.bilca.event.repository.EventRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events/{code}/documents")
public class EventDocumentController {

    private final EventRepository eventRepository;
    private final EventDocumentRepository eventDocumentRepository;
    private final AdminAuth adminAuth;

    public EventDocumentController(EventRepository eventRepository,
                                   EventDocumentRepository eventDocumentRepository,
                                   AdminAuth adminAuth) {
        this.eventRepository = eventRepository;
        this.eventDocumentRepository = eventDocumentRepository;
        this.adminAuth = adminAuth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                   @PathVariable String code,
                                   @RequestParam("file") MultipartFile file) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            fileName = "documento";
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        try {
            EventDocument document = eventDocumentRepository.save(event.id(), fileName, contentType, file.getBytes());
            return DocumentResponse.from(document);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado");
        }
    }

    @GetMapping
    public List<DocumentResponse> list(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                       @PathVariable String code) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        return eventDocumentRepository.findMetadataByEventId(event.id()).stream().map(DocumentResponse::from).toList();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                           @PathVariable String code, @PathVariable Long id) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        EventDocument metadata = eventDocumentRepository.findMetadataByIdAndEventId(id, event.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
        byte[] data = eventDocumentRepository.findDataByIdAndEventId(id, event.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(metadata.fileName()).build().toString())
                .body(data);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                       @PathVariable String code, @PathVariable Long id) {
        adminAuth.require(token);
        Event event = findEventOrThrow(code);
        int deleted = eventDocumentRepository.deleteByIdAndEventId(id, event.id());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado");
        }
    }

    private Event findEventOrThrow(String code) {
        return eventRepository.findByPublicCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
    }

    public record DocumentResponse(Long id, String fileName, String contentType, long fileSize,
                                   OffsetDateTime uploadedAt) {
        public static DocumentResponse from(EventDocument document) {
            return new DocumentResponse(document.id(), document.fileName(), document.contentType(),
                    document.fileSize(), document.uploadedAt());
        }
    }
}
