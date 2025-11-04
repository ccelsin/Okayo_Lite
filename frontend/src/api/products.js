import client from "./client";

// All Api calls around product

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

export async function updateProduct(payload) {
  const { data } = await client.put(`/api/product`, payload);
  return data;
}
