export type HealthResponse = {
  status: string;
  service: string;
};

export type ApiErrorResponse = {
  mensaje?: string;
  timestamp?: string;
  path?: string;
};
