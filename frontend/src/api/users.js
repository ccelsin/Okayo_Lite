import client from "./client";

//Api call around users

export async function getProfile() {
  const { data } = await client.get("/api/user/profile");
  return data;
}

export async function updateProfile(payload) {
  const { data } = await client.put("/api/user/profile", payload);
  return data;
}
