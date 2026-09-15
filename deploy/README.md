# Deploy publico na Oracle Cloud Infrastructure

Arquitetura: Internet -> Nginx HTTPS -> `java -jar` em `127.0.0.1:8080` (servico systemd) -> Oracle. Sem Docker: a VM e um `VM.Standard.E2.1.Micro` (1 OCPU / 1GB RAM, Always Free) e instalar Docker nela e pesado demais - ja travou a instancia uma vez. A porta 8080 e o banco nao sao publicos.

## 1. Preparar a VM

1. Crie uma VM Compute Oracle Linux 9 com IP publico e chave SSH.
2. No Network Security Group ou Security List, permita TCP 80 e 443 de qualquer origem. Permita SSH (22) apenas do seu IP. Nao abra 8080, 1521 ou 1522.
3. Aponte um registro DNS A, por exemplo `eventos.seudominio.com`, ao IP publico da VM (opcional - sem dominio, o site funciona so por HTTP no IP).
4. **Crie um swap antes de instalar qualquer coisa** (a VM so tem 1GB de RAM):
   ```
   sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   ```
5. Instale só o Java Runtime (JRE, não precisa do JDK completo) e o Nginx/Certbot:
   ```
   sudo dnf -y install java-21-openjdk-headless nginx certbot python3-certbot-nginx
   ```
6. Crie o usuario e as pastas da aplicacao:
   ```
   sudo groupadd -r bilca
   sudo useradd -r -g bilca -d /opt/bilca-event -s /sbin/nologin bilca
   sudo mkdir -p /opt/bilca-event/app /opt/bilca-event/wallet
   ```

## 2. Buildar e enviar a aplicacao

Na sua maquina (nao na VM - evita gastar RAM da VM com o build):

```
mvn -DskipTests package
scp target/bilca-event-0.0.1-SNAPSHOT.jar opc@SEU_IP:/tmp/app.jar
ssh opc@SEU_IP "sudo mv /tmp/app.jar /opt/bilca-event/app/app.jar && sudo chown bilca:bilca /opt/bilca-event/app/app.jar"
```

## 3. Configurar segredos, wallet e schema

1. Crie `/opt/bilca-event/.env` na VM a partir de `bilca-event.env.example`; preencha todos os segredos e execute `sudo chmod 600 /opt/bilca-event/.env` e `sudo chown bilca:bilca /opt/bilca-event/.env`.
2. Para o Autonomous Database, baixe a Wallet em **Database connection** na console OCI, envie o `.zip` pra VM (`scp`) e extraia em `/opt/bilca-event/wallet`. Depois: `sudo chown -R bilca:bilca /opt/bilca-event/wallet && sudo chmod 700 /opt/bilca-event/wallet`.
3. No `.env`, use o alias existente no `tnsnames.ora` da wallet (ex.: `bilcabd_medium`) na `ORACLE_DATASOURCE_URL`.
4. Crie o usuário `event_app` no banco (uma vez, como ADMIN) e rode `schema.sql` (uma vez, como `event_app`) — mais fácil pelo **Database Actions** no navegador (`Autonomous Database > BilcaBD > Database connection > Database Actions > SQL`) do que via sqlplus, já que a porta 1522 costuma estar bloqueada em redes corporativas.

## 4. Servico systemd

```
scp deploy/bilca-event.service opc@SEU_IP:/tmp/bilca-event.service
ssh opc@SEU_IP "sudo mv /tmp/bilca-event.service /etc/systemd/system/bilca-event.service && sudo systemctl daemon-reload && sudo systemctl enable --now bilca-event"
```

Verifique com `sudo systemctl status bilca-event` e `sudo journalctl -u bilca-event -f`.

## 5. Nginx + HTTPS

1. Copie `nginx-bilca-event.conf` para `/etc/nginx/conf.d/bilca-event.conf` na VM (Oracle Linux carrega tudo de `conf.d` automaticamente), troque todas as ocorrencias de `SEU_DOMINIO` e confira com `sudo nginx -t`, depois `sudo systemctl enable --now nginx`.
2. Se tiver dominio, emita o certificado: `sudo certbot --nginx -d SEU_DOMINIO`. Sem dominio, o site funciona por HTTP direto no IP.

## Atualizar a aplicacao depois

Repita só o passo 2 (build local + scp) e reinicie o servico: `ssh opc@SEU_IP "sudo systemctl restart bilca-event"`.

---

O URL para os participantes sera `https://SEU_DOMINIO/?evento=CODIGO_DO_EVENTO` (ou `http://SEU_IP/?evento=CODIGO_DO_EVENTO` sem dominio).

Para criar um evento pela API, envie o cabecalho `X-Admin-Token` com o valor de `BILCA_ADMIN_API_TOKEN`; a criacao de eventos nao deve ficar aberta ao publico.
