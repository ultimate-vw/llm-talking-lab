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
