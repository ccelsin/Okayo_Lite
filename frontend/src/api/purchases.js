import client from "./client";

export async function createPurchase(payload) {
  const { data } = await client.post("/api/purchase", payload);
  return data;
}

export async function listPurchases() {
  const { data } = await client.get("/api/purchase");
  return data;
}

export async function listMyPurchases() {
  const { data } = await client.get("/api/purchase/mine");
  return data;
}

export async function updatePurchase(id, payload) {
  const { data } = await client.put(`/api/purchase/${id}`, payload);
  return data;
}

export async function getPurchase(id) {
  const { data } = await client.get(`/api/purchase/${id}`);
  return data;
}
