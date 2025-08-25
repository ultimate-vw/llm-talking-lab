# LLM Talking Lab

The aim of this project is to explore various ways we are talking to the LLM these days. 

##  **Current Status: FULLY IMPLEMENTED & WORKING**

All features have been implemented with modern, interactive UIs:

-  **Chat** - Multiple communication modes with transport switching
-  **RAG** - Document search and retrieval with Lucene indexing
-  **Agent** - Tool-based reasoning with RAG integration
-  **Docs** - Document processing pipeline with real-time monitoring
-  **Voice** - WebSocket-based voice interactions

##  **What This Demonstrates**

**Spectrum of LLM User communication along four axes:**

-> **Timing**: synchronous vs. streaming vs. asynchronous.

-> **Channel**: web/app UI, chat apps (Slack/Teams), email/SMS, voice/calls, CLI.

-> **Transport**: REST, SSE, WebSocket, webhooks, long-polling, pub/sub.

-> **Response shape**: free text, JSON schema, function/tool calls, plan/steps, citations.

##  **Key Features**

### **1. Main Dashboard (Orchestrator Service)**
- **Transport Mode Switching** - Toggle between REST, SSE, WebSocket, Batch, and Long-polling
- **Performance Comparison** - Real-time metrics (TTFB, total time, tokens/sec, UX score)
- **Interactive Testing** - Same prompt, different transports for easy comparison

### **2. Specialized Services**
- **RAG Service** - Document ingestion, search, and retrieval
- **Agent Service** - Tool-based reasoning with streaming execution
- **Docs Service** - File processing pipeline with job monitoring
- **Voice Service** - Real-time WebSocket communication

##  **Quick Start**

### **1. Start the Stack**
```bash
# Build and start all services
docker compose up -d --build

# Pull Ollama model (first time only)
docker exec -it ollama ollama pull llama3.1
```

### **2. Access the UIs**

| Service | URL | Description |
|---------|-----|-------------|
| **Main Dashboard** | `http://localhost/` | Transport mode switching & comparison |
| **RAG Service** | `http://localhost/rag/` | Document search & retrieval |
| **Agent Service** | `http://localhost/agent/` | Tool-based reasoning |
| **Docs Service** | `http://localhost/docs/` | Document processing |
| **Voice Service** | `http://localhost/voice/` | WebSocket communication |

### **3. Test the Main Dashboard**
1. Open `http://localhost/` in your browser
2. Type a prompt (e.g., "Write a haiku about technology")
3. Click different transport buttons to see the same request handled differently
4. Compare performance metrics in real-time

##  **Testing Commands**

### **Core Communication Modes**

#### **REST API (Synchronous)**
```bash
curl -s http://localhost/api/v1/rest \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"Write a haiku about oceans"}'
```

#### **SSE (Streaming)**
```bash
curl -N "http://localhost/api/v1/sse?prompt=Count%20to%20five"
```

#### **WebSocket**
```bash
# Use the web interface or test with wscat
wscat -c ws://localhost/api/v1/ws
# Send: {"type":"user_prompt","prompt":"hello"}
```

#### **Batch Processing**
```bash
# Submit job
curl -s http://localhost/api/v1/batch \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"Process this request"}'

# Monitor job (replace JOB_ID with actual ID)
curl -N "http://localhost/api/v1/jobs/JOB_ID/events"
```

#### **Long-polling**
```bash
curl -s http://localhost/api/v1/longpoll \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"test message"}'
```

### **Specialized Services**

#### **RAG Service**
```bash
# Ingest documents
curl -X POST http://localhost/rag/ingest

# Query documents
curl "http://localhost/rag/query?q=your%20search%20term&k=5"
```

#### **Agent Service**
```bash
# Test agent with streaming
curl -N "http://localhost/agent/run-stream?message=calculate%202+2*3"
```

#### **Docs Service**
```bash
# Submit document for processing
curl -X POST http://localhost/docs/submit \
  -F "file=@/path/to/your/document.txt"

# Monitor processing (replace JOB_ID)
curl -N "http://localhost/docs/jobs/JOB_ID/events"
```

#### **Voice Service**
```bash
# Test WebSocket voice interface
wscat -c ws://localhost/voice/ws
# Send: "What time is it?"
```

##  **Health Check**

```bash
# Check all services are running
docker compose ps

# Check service logs
docker compose logs orchestrator
docker compose logs rag
docker compose logs agent
docker compose logs docs
docker compose logs voice

# Test service connectivity
curl -f http://localhost/ || echo "Main UI down"
curl -f http://localhost/rag/ || echo "RAG service down"
curl -f http://localhost/agent/ || echo "Agent service down"
curl -f http://localhost/docs/ || echo "Docs service down"
curl -f http://localhost/voice/ || echo "Voice service down"
```

##  **UI Features**

### **Transport Switching**
- **One-click mode switching** between REST, SSE, WebSocket, Batch, and Long-polling
- **Real-time performance metrics** for each mode
- **Side-by-side comparison** of different communication approaches

### **Modern Design**
- **Responsive layout** that works on all devices
- **Beautiful gradients** and smooth animations
- **Interactive elements** with hover effects and loading states
- **Professional appearance** suitable for demos and presentations

### **Service-Specific UIs**
- **RAG**: Drag-and-drop document ingestion, real-time search results
- **Agent**: Streaming conversation interface with tool execution visualization
- **Docs**: File upload with job monitoring and progress tracking
- **Voice**: WebSocket chat interface with connection status indicators

## ️ **Architecture**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Traefik       │    │   Ollama        │    │   Services      │
│   (Port 80)     │    │   (Port 11434)  │    │                 │
│                 │    │                 │    │ • Orchestrator  │
│ • Load Balancer │    │ • LLM Model     │    │ • RAG           │
│ • SSL/TLS       │    │ • Local/Remote  │    │ • Agent         │
│ • Routing       │    │                 │    │ • Docs          │
└─────────────────┘    └─────────────────┘    │ • Voice         │
         │                       │            └─────────────────┘
         └───────────────────────┘
```

##  **Configuration**

### **Ollama Settings**
Default configuration in `orchestrator-service/src/main/resources/application.yml`:

```yaml
ollama:
  url: http://ollama:11434
  model: llama3.1
```

Override via environment variables:
```bash
docker compose up -d orchestrator \
  -e OLLAMA_URL=http://ollama:11434 \
  -e OLLAMA_MODEL=llama3.1
```

### **Service Ports**
- **Main UI**: Port 80 (via Traefik)
- **Direct Access**: Ports 8081-8085 (for development)
- **Ollama**: Port 11434

## 📚 **Use Cases**

### **Educational**
- **Compare communication protocols** side-by-side
- **Understand streaming vs. batch** processing
- **Learn about different transport mechanisms**

### **Development**
- **Test LLM integration patterns**
- **Benchmark performance** of different approaches
- **Prototype new communication methods**

### **Demonstration**
- **Showcase modern UI design**
- **Present LLM capabilities** to stakeholders
- **Demo real-time streaming** applications

##  **What's Next?**

The project is now fully functional with:
- All core features implemented
- Modern, responsive UIs
- Transport mode switching
- Performance metrics
- Service integration

**Ready for production use, demos, and further development!**

---

**Note**: If you run Ollama outside Docker (native app), set `ollama.url` to `http://host.docker.internal:11434` so services in Docker can reach it.
