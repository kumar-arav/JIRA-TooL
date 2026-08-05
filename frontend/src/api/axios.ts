import axios from "axios";

const api = axios.create({
  baseURL: "https://jira-tool-1.onrender.com/api",
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(config => {
  const saved = localStorage.getItem("fs_auth");
  if (saved) {
    const { token } = JSON.parse(saved);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem("fs_auth");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;
