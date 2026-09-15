# Bilca Event

Site estático para cadastro de presença em palestras/eventos. Todos os participantes que se inscrevem concorrem aos brindes — não há sorteio, é confirmação de presença.

Sem backend, sem banco de dados: hospedado no GitHub Pages, e cada inscrição vira uma **issue** neste repositório (via [Issue Forms](https://docs.github.com/pt/issues/tracking-your-work-with-issues/using-issues/creating-an-issue-template-for-your-repository) do GitHub).

## Como funciona

1. Você cadastra um evento em [`events.json`](events.json).
2. O QR Code do evento aponta para `https://SEU_USUARIO.github.io/SEU_REPO/?evento=CODIGO`.
3. O participante abre o link, vê os dados do evento e clica em "Confirmar presença".
4. Isso abre uma nova issue neste repositório (o participante precisa estar logado no GitHub) já com o formulário de inscrição (nome, e-mail, telefone, stack) e com as labels `inscricao` e `evento-CODIGO`.

## Cadastrar um novo evento

Edite [`events.json`](events.json) e adicione um objeto novo:

```json
{
  "code": "meu-evento-2026",
  "name": "Nome do evento",
  "date": "2026-11-20",
  "host": "Seu nome"
}
```

Faça commit e push — o GitHub Pages atualiza automaticamente em alguns segundos.

## Ver os participantes de um evento

Vá em **Issues** neste repositório e filtre pela label `evento-CODIGO` (e `inscricao`). Cada issue é um participante inscrito.

## Publicar (primeira vez)

No repositório no GitHub: **Settings → Pages → Build and deployment → Deploy from a branch**, selecione a branch `main` e a pasta `/ (root)`.
