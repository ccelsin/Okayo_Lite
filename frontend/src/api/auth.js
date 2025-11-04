import client from "./client";

//Login and register api endpoints
export async function login(payload) {
  const { data } = await client.post("/api/auth/login", payload);
  return data;
}

export async function register(payload) {
  const { data } = await client.post("/api/auth/register", payload);
  return data;
}

export async function isAdmin() {
  const { data } = await client.get("/api/user/isAdmin");
  return Boolean(data);
}
