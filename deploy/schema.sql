-- Rode este script uma vez no usuário event_app (via Database Actions ou sqlplus),
-- não é executado automaticamente pela aplicação.

CREATE SEQUENCE events_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE participants_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE event_documents_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE events (
    id             NUMBER PRIMARY KEY,
    public_code    VARCHAR2(36) NOT NULL UNIQUE,
    name           VARCHAR2(150) NOT NULL,
    event_date     DATE NOT NULL,
    event_host     VARCHAR2(150) NOT NULL,
    event_location VARCHAR2(200) NOT NULL,
    capacity       NUMBER(6)
);

CREATE TABLE participants (
    id         NUMBER PRIMARY KEY,
    event_id   NUMBER NOT NULL REFERENCES events(id),
    name       VARCHAR2(150) NOT NULL,
    email      VARCHAR2(180) NOT NULL,
    phone      VARCHAR2(30) NOT NULL,
    tech_stack VARCHAR2(150) NOT NULL,
    materials_sent_at TIMESTAMP,
    CONSTRAINT uk_participant_event_email UNIQUE (event_id, email)
);

CREATE TABLE event_documents (
    id           NUMBER PRIMARY KEY,
    event_id     NUMBER NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    file_name    VARCHAR2(255) NOT NULL,
    content_type VARCHAR2(150) NOT NULL,
    file_size    NUMBER NOT NULL,
    file_data    BLOB NOT NULL,
    uploaded_at  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE INDEX ix_event_documents_event ON event_documents(event_id);
