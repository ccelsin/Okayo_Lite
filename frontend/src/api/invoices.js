import client from "./client";

export async function listInvoices() {
  const response = await client.get("/invoices");
  return response.data;
}

export async function getInvoice(id) {
  const response = await client.get(`/invoices/${id}`);
  return response.data;
}

export async function createInvoice(data) {
  const response = await client.post("/invoices", data);
  return response.data;
}

export async function updateInvoice(id, data) {
  const response = await client.put(`/invoices/${id}`, data);
  return response.data;
}

export async function downloadInvoicePdf(id) {
  const response = await client.get(`/invoices/${id}/pdf`, {
    responseType: 'blob'
  });
  return response.data;
}