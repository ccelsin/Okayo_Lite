import client from "./client";

export async function login(payload) {
  const { data } = await client.post("/api/auth/login", payload);
  return data;
}

export async function register(payload) {
  const { data } = await client.post("/api/auth/register", payload);
  return data;
}
