import client from "./client";

export async function listPaymentDetails() {
  const { data } = await client.get("/api/payment-details");
  return data;
}

export async function createPaymentDetails(payload) {
  const { data } = await client.post("/api/payment-details", payload);
  return data;
}

export async function updatePaymentDetails(id, payload) {
  const { data } = await client.put(`/api/payment-details/${id}`, payload);
  return data;
}

export async function getPaymentDetails(id) {
  const { data } = await client.get(`/api/payment-details/${id}`);
  return data;
}
