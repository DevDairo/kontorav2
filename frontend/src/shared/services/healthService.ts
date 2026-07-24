import { apiClient } from "./apiClient";
import type { HealthResponse } from "../types/api";

export function getBackendHealth() {
  return apiClient.get<HealthResponse>("/health");
}
