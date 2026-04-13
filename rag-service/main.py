from fastapi import FastAPI

app = FastAPI(title="Ascend RAG Service")

@app.get("/health")
def health():
    return {"status": "ok", "service": "rag"}