"""Sidecar de reconhecimento de especies para o ZooCard.

Usa BioCLIP 2 (imageomics/bioclip-2, licenca MIT) em modo zero-shot:
o embedding da imagem e comparado com os embeddings dos nomes candidatos
lidos de especies_brasil.txt. Restringir os candidatos a fauna brasileira
melhora bastante a acuracia e mitiga o vies geografico do modelo.
"""
import io
import logging
import pathlib

import open_clip
import torch
import torch.nn.functional as F
from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("zoocard-recognition")

MODEL_NAME = "hf-hub:imageomics/bioclip-2"
CANDIDATOS_PATH = pathlib.Path(__file__).parent / "especies_brasil.txt"
TOP_K = 5

app = FastAPI(title="ZooCard Recognition")

device = "cuda" if torch.cuda.is_available() else "cpu"
model, _, preprocess = open_clip.create_model_and_transforms(MODEL_NAME)
model = model.to(device).eval()
tokenizer = open_clip.get_tokenizer(MODEL_NAME)

especies = [
    linha.strip()
    for linha in CANDIDATOS_PATH.read_text(encoding="utf-8").splitlines()
    if linha.strip() and not linha.startswith("#")
]
log.info("Carregadas %d especies candidatas em %s", len(especies), device)

# Os embeddings de texto sao fixos: calculamos uma vez na subida.
with torch.no_grad():
    tokens = tokenizer([f"a photo of {nome}." for nome in especies]).to(device)
    text_features = model.encode_text(tokens)
    text_features = F.normalize(text_features, dim=-1)


@app.get("/health")
def health():
    return {"status": "ok", "device": device, "especies": len(especies)}


@app.post("/identify")
async def identify(file: UploadFile = File(...)):
    conteudo = await file.read()
    try:
        imagem = Image.open(io.BytesIO(conteudo)).convert("RGB")
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Imagem invalida: {exc}") from exc

    with torch.no_grad():
        tensor = preprocess(imagem).unsqueeze(0).to(device)
        image_features = model.encode_image(tensor)
        image_features = F.normalize(image_features, dim=-1)
        similaridades = (100.0 * image_features @ text_features.T).softmax(dim=-1)[0]

    top = torch.topk(similaridades, k=min(TOP_K, len(especies)))
    candidatos = [
        {"scientificName": especies[idx], "confidence": round(float(score), 4)}
        for score, idx in zip(top.values.tolist(), top.indices.tolist())
    ]

    return {
        "scientificName": candidatos[0]["scientificName"],
        "confidence": candidatos[0]["confidence"],
        "candidates": candidatos,
    }
