import axios from "axios";
import { getAuthToken } from "../utils/auth";

export const getRoles = async () => {
  const token = getAuthToken();

  const res = await axios.get("http://localhost:9090/roles", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  return res.data.content ?? res.data;
};