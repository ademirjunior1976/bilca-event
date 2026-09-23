package br.com.bilca.event.repository;

import br.com.bilca.event.domain.EventDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class EventDocumentRepository {

    private static final String METADATA_COLUMNS = "id, event_id, file_name, content_type, file_size, uploaded_at";

    private static final RowMapper<EventDocument> METADATA_ROW_MAPPER = (rs, rowNum) -> new EventDocument(
            rs.getLong("id"),
            rs.getLong("event_id"),
            rs.getString("file_name"),
            rs.getString("content_type"),
            rs.getLong("file_size"),
            ParticipantRepository.toOffsetDateTime(rs.getTimestamp("uploaded_at")));

    private final JdbcTemplate jdbcTemplate;

    public EventDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EventDocument save(Long eventId, String fileName, String contentType, byte[] data) {
        Long id = jdbcTemplate.queryForObject("SELECT event_documents_seq.NEXTVAL FROM dual", Long.class);
        jdbcTemplate.update(
                "INSERT INTO event_documents (id, event_id, file_name, content_type, file_size, file_data) VALUES (?, ?, ?, ?, ?, ?)",
                id, eventId, fileName, contentType, (long) data.length, data);
        return new EventDocument(id, eventId, fileName, contentType, data.length, OffsetDateTime.now());
    }

    public List<EventDocument> findMetadataByEventId(Long eventId) {
        return jdbcTemplate.query(
                "SELECT " + METADATA_COLUMNS + " FROM event_documents WHERE event_id = ? ORDER BY uploaded_at",
                METADATA_ROW_MAPPER, eventId);
    }

    public int countByEventId(Long eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_documents WHERE event_id = ?", Integer.class, eventId);
        return count == null ? 0 : count;
    }

    public Optional<EventDocument> findMetadataByIdAndEventId(Long id, Long eventId) {
        return jdbcTemplate.query(
                        "SELECT " + METADATA_COLUMNS + " FROM event_documents WHERE id = ? AND event_id = ?",
                        METADATA_ROW_MAPPER, id, eventId)
                .stream().findFirst();
    }

    public Optional<byte[]> findDataByIdAndEventId(Long id, Long eventId) {
        return jdbcTemplate.query(
                        "SELECT file_data FROM event_documents WHERE id = ? AND event_id = ?",
                        (rs, rowNum) -> rs.getBytes("file_data"), id, eventId)
                .stream().findFirst();
    }

    public int deleteByIdAndEventId(Long id, Long eventId) {
        return jdbcTemplate.update(
                "DELETE FROM event_documents WHERE id = ? AND event_id = ?", id, eventId);
    }
}
