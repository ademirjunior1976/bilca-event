-- Migracao do banco que ja esta em producao (rode uma vez, como event_app, pelo Database Actions).
-- Instalacoes novas nao precisam disto: schema.sql ja inclui tudo.

-- Documentos por evento (enviados por e-mail aos participantes)
CREATE SEQUENCE event_documents_seq START WITH 1 INCREMENT BY 1;

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

-- Controle de quem ja recebeu o material
ALTER TABLE participants ADD (materials_sent_at TIMESTAMP);

-- Capacidade total do evento (base do grafico de inscritos no admin)
ALTER TABLE events ADD (capacity NUMBER(6));
