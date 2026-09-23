package br.com.bilca.event.repository;

import br.com.bilca.event.domain.Participant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ParticipantRepository {

    private static final String SELECT_COLUMNS = "id, event_id, name, email, phone, tech_stack, materials_sent_at";

    private static final RowMapper<Participant> ROW_MAPPER = (rs, rowNum) -> new Participant(
            rs.getLong("id"),
            rs.getLong("event_id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("tech_stack"),
            toOffsetDateTime(rs.getTimestamp("materials_sent_at")));

    private final JdbcTemplate jdbcTemplate;

    public ParticipantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(OffsetDateTime.now().getOffset());
    }

    public boolean existsByEventIdAndEmailIgnoreCase(Long eventId, String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM participants WHERE event_id = ? AND UPPER(email) = UPPER(?)",
                Integer.class, eventId, email);
        return count != null && count > 0;
    }

    public boolean existsByEventIdAndEmailIgnoreCaseExcludingId(Long eventId, String email, Long excludeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM participants WHERE event_id = ? AND UPPER(email) = UPPER(?) AND id != ?",
                Integer.class, eventId, email, excludeId);
        return count != null && count > 0;
    }

    public Participant save(Long eventId, String name, String email, String phone, String stack) {
        Long id = jdbcTemplate.queryForObject("SELECT participants_seq.NEXTVAL FROM dual", Long.class);
        jdbcTemplate.update(
                "INSERT INTO participants (id, event_id, name, email, phone, tech_stack) VALUES (?, ?, ?, ?, ?, ?)",
                id, eventId, name, email, phone, stack);
        return new Participant(id, eventId, name, email, phone, stack, null);
    }

    public int countByEventId(Long eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM participants WHERE event_id = ?", Integer.class, eventId);
        return count == null ? 0 : count;
    }

    public List<Participant> findByEventId(Long eventId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM participants WHERE event_id = ? ORDER BY id DESC",
                ROW_MAPPER, eventId);
    }

    public Optional<Participant> findByIdAndEventId(Long id, Long eventId) {
        return jdbcTemplate.query(
                        "SELECT " + SELECT_COLUMNS + " FROM participants WHERE id = ? AND event_id = ?",
                        ROW_MAPPER, id, eventId)
                .stream().findFirst();
    }

    public int deleteByIdAndEventId(Long id, Long eventId) {
        return jdbcTemplate.update(
                "DELETE FROM participants WHERE id = ? AND event_id = ?", id, eventId);
    }

    public int update(Long id, Long eventId, String name, String email, String phone, String stack) {
        return jdbcTemplate.update(
                "UPDATE participants SET name = ?, email = ?, phone = ?, tech_stack = ? WHERE id = ? AND event_id = ?",
                name, email, phone, stack, id, eventId);
    }

    public int markMaterialsSent(Long id, Long eventId, boolean sent) {
        return jdbcTemplate.update(
                "UPDATE participants SET materials_sent_at = ? WHERE id = ? AND event_id = ?",
                sent ? Timestamp.from(Instant.now()) : null, id, eventId);
    }
}
