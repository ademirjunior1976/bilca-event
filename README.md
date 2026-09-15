# Bilca Event

MVP para cadastro de eventos e participantes usando Java, Spring Boot e Oracle.

## Executar localmente

Defina as variáveis de conexão do Oracle:

```powershell
$env:ORACLE_DATASOURCE_URL="jdbc:oracle:thin:@localhost:1521/XEPDB1"
$env:ORACLE_DATASOURCE_USERNAME="event_app"
$env:ORACLE_DATASOURCE_PASSWORD="sua-senha"
mvn spring-boot:run
```

Depois acesse `http://localhost:8080`.

## API

Criar evento:

```http
POST /api/events
Content-Type: application/json
X-Admin-Token: seu-token-administrativo

{
  "name": "Java e Oracle na prática",
  "date": "2026-10-10",
  "host": "Seu nome"
}
```

O campo `code` retornado identifica o evento. A página pública fica em `https://seu-dominio/?evento=CODE`.

## Oracle Autonomous Database na OCI

Para o Autonomous Database, envie a Wallet para a VM da aplicação e configure a URL usando o serviço do arquivo `tnsnames.ora`, por exemplo:

```text
jdbc:oracle:thin:@bilca_high?TNS_ADMIN=/opt/bilca-event/wallet
```

Nunca exponha a porta do Oracle para a internet. Libere acesso ao banco somente a partir da VM da aplicação e mantenha as credenciais em variáveis de ambiente ou em um serviço de segredos.
