The aim of this project is to explore various ways we are talking to the LLM these days. 

Making initial commit. 


TO DO:

chat
rag
agent
docs
voice


demonstrating the different ways an LLM can communicate with a user

Spectrum of LLM User communication along four axes:

-> Timing: synchronous vs. streaming vs. asynchronous.

-> Channel: web/app UI, chat apps (Slack/Teams), email/SMS, voice/calls, CLI.

-> Transport: REST, SSE, WebSocket, webhooks, long-polling, pub/sub.

-> Response shape: free text, JSON schema, function/tool calls, plan/steps, citations.

Below are concrete demo options (no code blobs), what each proves, and how we’d present it. Pick the set that matches your audience—then we wire up only those.


Option A — “Modes Gallery” (single page, switchable transports)

Purpose: One question, many pipes. Flip a toggle to see the same prompt delivered via different transports.

Transports shown (side-by-side):

REST (sync) → single JSON reply

SSE (streaming) → token/chunk stream with a moving caret

WebSocket (duplex) → interleaved plan → tool → observation → partial answer

Long-polling → legacy-friendly fallback

Batch + Webhook → job accepted → browser receives webhook echo (or SSE monitor)

Pub/Sub (optional) → broadcast updates to multiple clients

What the user sees: identical prompt box + a toggle for the transport. A latency bar shows:

TTFB (first byte/token), time-to-useful, total.

A log rail (event timeline): sent, first_token, tool_call, webhook_received, etc.

What this proves: the communication contract is separable from the model; you can choose per use-case.


## Run locally with Ollama (local LLM)

1) Install Ollama on your machine (see `https://ollama.com`), or use the included docker-compose service.

2) Bring up the stack with Ollama:

```bash
docker compose up -d --build
```

This will also start an `ollama` container on port 11434 with a persistent volume.

3) Pull a model (first time only). From your host:

```bash
docker exec -it ollama ollama pull llama3.1
```

4) Configure model and endpoint (optional). Defaults are set in `orchestrator-service/src/main/resources/application.yml`:

```yaml
ollama:
  url: http://ollama:11434
  model: llama3.1
```

You can override via env vars when starting the orchestrator:

```bash
docker compose up -d orchestrator \
  -e OLLAMA_URL=http://ollama:11434 \
  -e OLLAMA_MODEL=llama3.1
```

5) Try the APIs:

- REST sync:

```bash
curl -s http://localhost:8081/api/v1/rest -H 'Content-Type: application/json' \
  -d '{"prompt":"Write a haiku about oceans"}'
```

- SSE streaming:

Open `http://localhost:8081` in the browser and use the SSE demo, or:

```bash
curl -N "http://localhost:8081/api/v1/sse?prompt=Count%20to%20five"
```

Notes:
- If you run Ollama outside docker (native app), set `ollama.url` to `http://host.docker.internal:11434` so services in Docker can reach it.
- First generation with a new model downloads weights and may be slow.
