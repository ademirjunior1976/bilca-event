package br.com.bilca.event.repository;

import br.com.bilca.event.domain.Event;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EventRepository {

    private static final RowMapper<Event> ROW_MAPPER = (rs, rowNum) -> new Event(
            rs.getLong("id"),
            rs.getString("public_code"),
            rs.getString("name"),
            rs.getDate("event_date").toLocalDate(),
            rs.getString("event_host"),
            rs.getString("event_location"));

    private final JdbcTemplate jdbcTemplate;

    public EventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Event save(String name, LocalDate date, String host, String location) {
        Long id = jdbcTemplate.queryForObject("SELECT events_seq.NEXTVAL FROM dual", Long.class);
        String publicCode = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO events (id, public_code, name, event_date, event_host, event_location) VALUES (?, ?, ?, ?, ?, ?)",
                id, publicCode, name, Date.valueOf(date), host, location);
        return new Event(id, publicCode, name, date, host, location);
    }

    public Optional<Event> findByPublicCode(String publicCode) {
        return jdbcTemplate.query(
                        "SELECT id, public_code, name, event_date, event_host, event_location FROM events WHERE public_code = ?",
                        ROW_MAPPER, publicCode)
                .stream().findFirst();
    }

    public List<Event> findAll() {
        return jdbcTemplate.query(
                "SELECT id, public_code, name, event_date, event_host, event_location FROM events ORDER BY id DESC",
                ROW_MAPPER);
    }

    public int update(String publicCode, String name, LocalDate date, String host, String location) {
        return jdbcTemplate.update(
                "UPDATE events SET name = ?, event_date = ?, event_host = ?, event_location = ? WHERE public_code = ?",
                name, Date.valueOf(date), host, location, publicCode);
    }

    public int deleteByPublicCode(String publicCode) {
        return jdbcTemplate.update("DELETE FROM events WHERE public_code = ?", publicCode);
    }
}
