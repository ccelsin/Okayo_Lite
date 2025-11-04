import client from "./client";

export async function createPurchase(payload) {
  const { data } = await client.post("/api/purchase", payload);
  return data;
}

export async function listPurchases() {
  const { data } = await client.get("/api/purchase/pending");
  return data;
}

export async function listPurchasesByCustomer(customerId) {
  const { data } = await client.get(`/api/purchase/${customerId}/customer`);
  return data;
}

export async function listPurchasesByCustomerPending(customerId) {
  const { data } = await client.get(`/api/purchase/${customerId}/customer/pending`);
  return data;
}

export async function listMyPurchases() {
  const { data } = await client.get("/api/purchase/mine");
  return data;
}

export async function updatePurchase(payload) {
  const { data } = await client.put(`/api/purchase`, payload);
  return data;
}

export async function getPurchase(id) {
  const { data } = await client.get(`/api/purchase/${id}`);
  return data;
}
