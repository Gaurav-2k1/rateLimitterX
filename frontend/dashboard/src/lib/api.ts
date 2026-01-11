import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' }
});

// Request interceptor
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const { data } = await axios.post(
            `${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/auth/refresh`,
            { refreshToken }
          );
          localStorage.setItem('accessToken', data.data.accessToken);
          error.config.headers.Authorization = `Bearer ${data.data.accessToken}`;
          return axios(error.config);
        } catch {
          localStorage.clear();
          window.location.href = '/login';
        }
      } else {
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  register: (email: string, password: string) => 
    api.post('/auth/register', { email, password }),
  login: (email: string, password: string) => 
    api.post('/auth/login', { email, password }),
  refresh: (refreshToken: string) => 
    api.post('/auth/refresh', { refreshToken }),
};

export const rulesApi = {
  getAll: () => api.get('/rules'),
  create: (rule: any) => api.post('/rules', rule),
  update: (id: string, rule: any) => api.put(`/rules/${id}`, rule),
  delete: (id: string) => api.delete(`/rules/${id}`),
};

export const apiKeysApi = {
  getAll: () => api.get('/api-keys'),
  create: (data: any) => api.post('/api-keys', data),
  delete: (id: string) => api.delete(`/api-keys/${id}`),
  rotate: (id: string) => api.post(`/api-keys/${id}/rotate`),
};

export const analyticsApi = {
  getRealtime: () => api.get('/analytics/realtime'),
  getTrends: (start: string, end: string) => 
    api.get(`/analytics/trends?start=${start}&end=${end}`),
  getTopIdentifiers: (limit: number = 10) => 
    api.get(`/analytics/top-identifiers?limit=${limit}`),
  getHourly: (hours: number = 24) => 
    api.get(`/analytics/hourly?hours=${hours}`),
  getLatencyTrends: (intervals: number = 12) => 
    api.get(`/analytics/latency-trends?intervals=${intervals}`),
  getRecentActivity: (limit: number = 10) => 
    api.get(`/analytics/recent-activity?limit=${limit}`),
};

export const alertsApi = {
  getAll: () => api.get('/alerts'),
  create: (alert: any) => api.post('/alerts', alert),
  delete: (id: string) => api.delete(`/alerts/${id}`),
};

export const bulkApi = {
  export: (format: string = 'json') => 
    api.get(`/bulk/export?format=${format}`, { responseType: 'blob' }),
  import: (file: File, format: string = 'json') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('format', format);
    return api.post('/bulk/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
};

export default api;

