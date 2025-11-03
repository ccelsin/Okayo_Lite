import client from "./client";

export async function listProducts() {
  const { data } = await client.get("/api/product");
  return data;
}

export async function getProduct(id) {
  const { data } = await client.get(`/api/product/${id}`);
  return data;
}

export async function createProduct(payload) {
  const { data } = await client.post("/api/product", payload);
  return data;
}

export async function updateProduct(id, payload) {
  const { data } = await client.put(`/api/product/${id}`, payload);
  return data;
}
