package br.com.bilca.event.repository;

import br.com.bilca.event.domain.Participant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ParticipantRepository {

    private static final RowMapper<Participant> ROW_MAPPER = (rs, rowNum) -> new Participant(
            rs.getLong("id"),
            rs.getLong("event_id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("tech_stack"));

    private final JdbcTemplate jdbcTemplate;

    public ParticipantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByEventIdAndEmailIgnoreCase(Long eventId, String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM participants WHERE event_id = ? AND UPPER(email) = UPPER(?)",
                Integer.class, eventId, email);
        return count != null && count > 0;
    }

    public Participant save(Long eventId, String name, String email, String phone, String stack) {
        Long id = jdbcTemplate.queryForObject("SELECT participants_seq.NEXTVAL FROM dual", Long.class);
        jdbcTemplate.update(
                "INSERT INTO participants (id, event_id, name, email, phone, tech_stack) VALUES (?, ?, ?, ?, ?, ?)",
                id, eventId, name, email, phone, stack);
        return new Participant(id, eventId, name, email, phone, stack);
    }

    public int countByEventId(Long eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM participants WHERE event_id = ?", Integer.class, eventId);
        return count == null ? 0 : count;
    }

    public List<Participant> findByEventId(Long eventId) {
        return jdbcTemplate.query(
                "SELECT id, event_id, name, email, phone, tech_stack FROM participants WHERE event_id = ? ORDER BY id DESC",
                ROW_MAPPER, eventId);
    }

    public int deleteByIdAndEventId(Long id, Long eventId) {
        return jdbcTemplate.update(
                "DELETE FROM participants WHERE id = ? AND event_id = ?", id, eventId);
    }
}
