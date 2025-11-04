import client from "./client";

export async function listInvoices() {
  const response = await client.get("api/invoices");
  return response.data;
}

export async function getInvoice(id) {
  const response = await client.get(`api/invoices/${id}`);
  return response.data;
}

export async function createInvoice(data) {
  const response = await client.post("api/invoices", data);
  return response.data;
}

export async function updateInvoice(id,data) {
  const response = await client.put(`api/invoices/${id}`, data);
  return response.data;
}

export async function downloadInvoicePdf(id) {
  const response = await client.get(`api/invoices/${id}/pdf`, {
    responseType: 'blob'
  });
  return response.data;
}