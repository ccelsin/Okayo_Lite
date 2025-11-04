import client from "./client";

export async function listTva() {
  const { data } = await client.get("/api/tva");
  return data;
}

export async function getTva(id) {
  const { data } = await client.get(`/api/tva/${id}`);
  return data;
}

export async function createTva(payload) {
  const { data } = await client.post("/api/tva", payload);
  return data;
}

export async function updateTva(payload) {
  const { data } = await client.put(`/api/tva`, payload);
  return data;
}

export async function deleteTva(id) {
  await client.delete(`/api/tva/${id}`);
}
