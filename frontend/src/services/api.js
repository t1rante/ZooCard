// ============================================================================
// services/api.js — ÚNICA fonte de dados do frontend.
// Agora fala com o backend Spring Boot real. Nenhuma tela precisou mudar.
// ============================================================================

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const TOKEN_KEY = 'zoocard:token';

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

async function request(path, { method = 'GET', body, isMultipart = false } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (!isMultipart && body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: isMultipart ? body : body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 || res.status === 403) {
    throw new Error('Usuário não autenticado.');
  }
  if (!res.ok) {
    // o backend devolve {message} para erro de regra de negocio; as telas
    // renderizam err.message cru, entao a string precisa chegar intacta
    let mensagem = 'Não foi possível completar a operação.';
    try {
      const erro = await res.json();
      if (erro?.message) mensagem = erro.message;
    } catch {
      // resposta sem corpo JSON — mantem a mensagem padrao
    }
    throw new Error(mensagem);
  }
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

// --- auth -------------------------------------------------------------------

export async function login(loginValue, password) {
  const data = await request('/auth/login', {
    method: 'POST',
    body: { login: loginValue, password },
  });
  setToken(data.token);
  return data.user;
}

export async function register({ nome, login: loginValue, password }) {
  await request('/auth/register', {
    method: 'POST',
    body: { nome, login: loginValue, password, role: 'USER', colecao: [] },
  });
  return login(loginValue, password);
}

export async function getCurrentUser() {
  if (!getToken()) return null;
  try {
    return await request('/auth/me');
  } catch {
    setToken(null);
    return null;
  }
}

export async function logout() {
  setToken(null);
}

// --- coleções ---------------------------------------------------------------

export async function listColecoes() {
  const user = await getCurrentUser();
  if (!user) throw new Error('Usuário não autenticado.');
  return request(`/colecoes/usuario/${user.id}`);
}

export async function getColecao(colecaoId) {
  return request(`/colecoes/${colecaoId}`);
}

export async function createColecao(nome) {
  const user = await getCurrentUser();
  if (!user) throw new Error('Usuário não autenticado.');
  return request(`/colecoes/usuario/${user.id}`, { method: 'POST', body: { nome } });
}

export async function duplicateColecao(colecaoId) {
  return request(`/colecoes/${colecaoId}/duplicar`, { method: 'POST' });
}

export async function deleteColecao(colecaoId) {
  return request(`/colecoes/${colecaoId}`, { method: 'DELETE' });
}

// --- cartas -----------------------------------------------------------------

export async function listCartasDaColecao(colecaoId) {
  const colecao = await getColecao(colecaoId);
  const cartaIds = colecao?.cartaIds ?? [];
  return Promise.all(cartaIds.map((id) => getCarta(id)));
}

export async function getCarta(cartaId) {
  return request(`/cartas/${cartaId}`);
}

export async function generateCarta(fotoFile) {
  const form = new FormData();
  form.append('foto', fotoFile);
  return request('/cartas/gerar', { method: 'POST', body: form, isMultipart: true });
}

export async function addCartaToColecoes(cartaId, colecaoIds) {
  for (const colecaoId of colecaoIds) {
    const colecao = await getColecao(colecaoId);
    const atuais = colecao.cartaIds ?? [];
    if (atuais.includes(cartaId)) continue; // idempotente
    await request(`/colecoes/${colecaoId}`, {
      method: 'PUT',
      body: { cartas: [...atuais, cartaId].map((id) => ({ id })) },
    });
  }
}

export async function removeCartaDaColecao(cartaId, colecaoId) {
  return request(`/colecoes/${colecaoId}/cartas/${cartaId}`, { method: 'DELETE' });
}

// --- perfil -----------------------------------------------------------------

export async function getProfileStats() {
  const user = await getCurrentUser();
  if (!user) throw new Error('Usuário não autenticado.');
  const colecoes = await listColecoes();
  const geral = colecoes.find((c) => c.isGeral);
  return {
    nome: user.nome,
    numColecoes: colecoes.length,
    numCartas: geral?.cartaIds?.length ?? 0,
  };
}
