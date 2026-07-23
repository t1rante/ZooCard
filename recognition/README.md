# Sidecar de reconhecimento — ZooCard

BioCLIP 2 (MIT) em modo zero-shot sobre a lista de `especies_brasil.txt`.

## Rodando

    python -m venv .venv
    source .venv/bin/activate
    pip install -r requirements.txt
    uvicorn app:app --host 127.0.0.1 --port 8000

O primeiro start baixa ~1,2 GB de pesos do Hugging Face e demora alguns minutos.
Sem GPU o modelo roda em CPU (alguns segundos por imagem), o que basta para a demo.

## Testando

    curl -F "file=@onca.jpg" http://127.0.0.1:8000/identify

## Ampliando a cobertura

Acrescente nomes cientificos em `especies_brasil.txt`, um por linha, e reinicie.
Quanto menor e mais focada a lista, maior a acuracia — o modelo escolhe sempre o
candidato mais proximo, entao especies fora da lista serao classificadas errado.
