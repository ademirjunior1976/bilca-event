# Deploy publico na Oracle Cloud Infrastructure

Arquitetura: Internet -> Caddy HTTPS -> `java -jar` em `127.0.0.1:8080` (servico systemd) -> Oracle. Sem Docker: a VM e um `VM.Standard.E2.1.Micro` (1 OCPU / ~500MB RAM uteis, Always Free) e instalar Docker nela e pesado demais - ja travou a instancia uma vez. A porta 8080 e o banco nao sao publicos.

**Importante: nao use `dnf install` para nada pesado nessa VM.** O proprio `dnf` (resolvedor de dependencias em Python) precisa de ~350MB residentes so pra rodar, e a VM so tem ~500MB uteis no total - mesmo com swap em disco configurado, `dnf install java-21-openjdk-headless` e `dnf install nginx` foram mortos por OOM repetidamente. A solucao que funcionou: baixar binarios prontos (JRE tarball, Caddy) direto via `curl` e instalar manualmente, sem `dnf`, para tudo que for pesado. Tambem desative o timer `dnf-makecache.timer` (`sudo systemctl disable --now dnf-makecache.timer`) - ele roda sozinho em background e chegou a consumir 140MB de RAM sem aviso.

Arquivos que o SELinux (enforcing por padrao no Oracle Linux 9) pode bloquear silenciosamente depois de um `scp`/`mv` a partir de `/tmp`: rode `sudo restorecon -Rv <caminho>` em qualquer binario, unit file ou config copiado manualmente (systemd rejeita units com contexto `user_tmp_t` dizendo "Unit file does not exist", e um binario/arquivo com contexto errado falha com "Permission denied" ao executar).

## 1. Preparar a VM

1. Crie uma VM Compute Oracle Linux 9 com IP publico e chave SSH.
2. No Network Security Group ou Security List, permita TCP 80 e 443 de qualquer origem. Permita SSH (22) apenas do seu IP. Nao abra 8080, 1521 ou 1522. **Isso nao basta**: o Oracle Linux 9 tambem tem o `firewalld` ativo localmente na VM bloqueando tudo que nao for `ssh`/`dhcpv6-client` por padrao - libere http/https nele tambem, senao a Security List libera mas a porta continua fechada:
   ```
   sudo firewall-cmd --permanent --add-service=http && sudo firewall-cmd --permanent --add-service=https && sudo firewall-cmd --reload
   ```
3. Aponte um registro DNS A, por exemplo `eventos.seudominio.com`, ao IP publico da VM (opcional - sem dominio, o site funciona so por HTTP no IP, mas troque o Caddyfile para o bloco `:80`, ja que o Caddy so emite HTTPS automatico quando tem dominio).
4. **Crie swap antes de instalar qualquer coisa** (a VM so tem ~500MB uteis de RAM) - disco (persistente) + zram (RAM comprimida, mais rapida, usada primeiro):
   ```
   sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   echo 'vm.swappiness=100' | sudo tee /etc/sysctl.d/99-swappiness.conf && sudo sysctl -p /etc/sysctl.d/99-swappiness.conf

   sudo modprobe zram
   echo 512M | sudo tee /sys/block/zram0/disksize
   sudo mkswap /dev/zram0 && sudo swapon -p 100 /dev/zram0
   ```
   O zram nao persiste sozinho num reboot (precisa recriar o modprobe/disksize/swapon depois de reiniciar, ou configurar `systemd-zram-setup@` se quiser automatizar).
5. Instale o Java (JRE) e o Caddy baixando os binarios prontos, sem `dnf`:
   ```
   curl -L -o /tmp/jre21.tar.gz 'https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jre/hotspot/normal/eclipse?project=jdk'
   sudo mkdir -p /opt/java21 && sudo tar xzf /tmp/jre21.tar.gz -C /opt/java21 --strip-components=1
   sudo ln -sf /opt/java21/bin/java /usr/bin/java
   rm /tmp/jre21.tar.gz

   CADDY_URL=$(curl -s https://api.github.com/repos/caddyserver/caddy/releases/latest | grep -oE '"browser_download_url": *"[^"]*linux_amd64\.tar\.gz"' | cut -d'"' -f4)
   curl -sL -o /tmp/caddy.tar.gz "$CADDY_URL"
   cd /tmp && tar xzf caddy.tar.gz caddy && sudo mv caddy /usr/bin/caddy && sudo chown root:root /usr/bin/caddy && sudo chmod 755 /usr/bin/caddy
   sudo setcap 'cap_net_bind_service=+ep' /usr/bin/caddy
   rm -f /tmp/caddy.tar.gz
   ```
6. Crie o usuario e as pastas da aplicacao:
   ```
   sudo groupadd -r bilca
   sudo useradd -r -g bilca -d /opt/bilca-event -s /sbin/nologin bilca
   sudo mkdir -p /opt/bilca-event/app /opt/bilca-event/wallet

   sudo groupadd -r caddy
   sudo useradd -r -g caddy -d /var/lib/caddy -s /sbin/nologin caddy
   sudo mkdir -p /etc/caddy /var/lib/caddy && sudo chown caddy:caddy /var/lib/caddy
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
3. No `.env`, use o alias existente no `tnsnames.ora` da wallet (ex.: `bilcabd_medium`) na `ORACLE_DATASOURCE_URL`. O `pom.xml` precisa da dependência `com.oracle.database.security:oraclepki` (mesma versão do `ojdbc11`) para autenticar via wallet SSO (`cwallet.sso`) — sem ela, a aplicação falha ao subir com `NoSuchAlgorithmException: SSO KeyStore not available`.
4. Crie o usuário `event_app` no banco (uma vez, como ADMIN) e rode `schema.sql` (uma vez, como `event_app` — ou como ADMIN com `ALTER SESSION SET CURRENT_SCHEMA = event_app` antes) — mais fácil pelo **Database Actions** no navegador (`Autonomous Database > BilcaBD > Database Actions > SQL`) do que via sqlplus, já que a porta 1522 costuma estar bloqueada em redes corporativas.
5. Se o Autonomous Database tiver **ACL habilitada** (Rede > Tipo de acesso "Permitir acesso seguro de IPs e VCN especificados"), adicione o **IP público da VM** em **Autonomous Database > Rede > Lista de controle de acesso > Editar** — só o OCID da VCN não basta quando a VM tem IP público direto (não passa por NAT Gateway). Sem isso a app falha com `ORA-12506: TNS:listener rejected connection based on service ACL filtering`. Ao editar a ACL pelo navegador, adicione também o seu próprio IP (tem um botão pronto pra isso) para não ficar bloqueado do Database Actions.

## 4. Servico systemd

```
scp deploy/bilca-event.service opc@SEU_IP:/tmp/bilca-event.service
ssh opc@SEU_IP "sudo mv /tmp/bilca-event.service /etc/systemd/system/bilca-event.service && sudo systemctl daemon-reload && sudo systemctl enable --now bilca-event"
```

Verifique com `sudo systemctl status bilca-event` e `sudo journalctl -u bilca-event -f`.

## 5. Caddy + HTTPS

1. Copie `Caddyfile` para `/etc/caddy/Caddyfile` na VM, troque `SEU_DOMINIO` pelo dominio real (ou use o bloco `:80` comentado se nao tiver dominio) e ajuste a posse: `sudo chown caddy:caddy /etc/caddy/Caddyfile`.
2. Copie `caddy.service` para `/etc/systemd/system/caddy.service` e rode `sudo systemctl daemon-reload && sudo systemctl enable --now caddy`.
3. Com dominio configurado, o Caddy emite e renova o certificado HTTPS automaticamente na primeira subida (sem passo manual de certbot). Verifique com `sudo systemctl status caddy` e `sudo journalctl -u caddy -f`.

## Atualizar a aplicacao depois

Repita só o passo 2 (build local + scp) e reinicie o servico: `ssh opc@SEU_IP "sudo systemctl restart bilca-event"`.

---

O URL para os participantes sera `https://SEU_DOMINIO/?evento=CODIGO_DO_EVENTO` (ou `http://SEU_IP/?evento=CODIGO_DO_EVENTO` sem dominio).

Para criar um evento pela API, envie o cabecalho `X-Admin-Token` com o valor de `BILCA_ADMIN_API_TOKEN`; a criacao de eventos nao deve ficar aberta ao publico.
