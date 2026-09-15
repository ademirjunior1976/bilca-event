# Deploy publico na Oracle Cloud Infrastructure

Arquitetura: Internet -> Nginx HTTPS -> aplicacao em `127.0.0.1:8080` -> Oracle. A porta 8080 e o banco nao sao publicos.

1. Crie uma VM Compute Oracle Linux 9 com IP publico e chave SSH.
2. No Network Security Group ou Security List, permita TCP 80 e 443 de qualquer origem. Permita SSH (22) apenas do seu IP. Nao abra 8080, 1521 ou 1522.
3. Aponte um registro DNS A, por exemplo `eventos.seudominio.com`, ao IP publico da VM.
4. Instale Docker, Docker Compose, Nginx e Certbot na VM (via `dnf`, nao `apt` - Oracle Linux). Copie este projeto para `/opt/bilca-event/app`.
5. Crie `/opt/bilca-event/.env` a partir de `bilca-event.env.example`; preencha todos os segredos e execute `chmod 600 /opt/bilca-event/.env`.
6. Para Autonomous Database com mTLS, baixe o Wallet em **Database connection** na console OCI, extraia em `/opt/bilca-event/wallet`, execute `sudo chown -R 10001:10001 /opt/bilca-event/wallet` e `sudo chmod 700 /opt/bilca-event/wallet`, e utilize no `.env` o alias existente em `tnsnames.ora`.
7. Na pasta `/opt/bilca-event/app`, execute `sudo docker compose up -d --build`.
8. Copie `nginx-bilca-event.conf` para `/etc/nginx/conf.d/bilca-event.conf` (Oracle Linux carrega tudo de `conf.d` automaticamente, sem `sites-available`/`sites-enabled`), troque todas as ocorrencias de `SEU_DOMINIO` e confira com `sudo nginx -t`.
9. Emita o certificado: `sudo certbot --nginx -d SEU_DOMINIO`.

O URL para os participantes sera `https://SEU_DOMINIO/?evento=CODIGO_DO_EVENTO`.

Para criar um evento pela API, envie o cabecalho `X-Admin-Token` com o valor de `BILCA_ADMIN_API_TOKEN`; a criacao de eventos nao deve ficar aberta ao publico.
