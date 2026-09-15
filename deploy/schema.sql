-- Rode este script uma vez no usuário event_app (via Database Actions ou sqlplus),
-- não é executado automaticamente pela aplicação.

CREATE SEQUENCE events_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE participants_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE events (
    id          NUMBER PRIMARY KEY,
    public_code VARCHAR2(36) NOT NULL UNIQUE,
    name        VARCHAR2(150) NOT NULL,
    event_date  DATE NOT NULL,
    event_host  VARCHAR2(150) NOT NULL
);

CREATE TABLE participants (
    id         NUMBER PRIMARY KEY,
    event_id   NUMBER NOT NULL REFERENCES events(id),
    name       VARCHAR2(150) NOT NULL,
    email      VARCHAR2(180) NOT NULL,
    phone      VARCHAR2(30) NOT NULL,
    tech_stack VARCHAR2(150) NOT NULL,
    CONSTRAINT uk_participant_event_email UNIQUE (event_id, email)
);
