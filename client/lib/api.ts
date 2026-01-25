import { ApiResponse, AuthResponse, User, Repository, AnalysisResult, AnalysisRequest } from './types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

class ApiClient {
  private token: string | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      this.token = localStorage.getItem('token');
    }
  }

  setToken(token: string | null) {
    this.token = token;
    if (typeof window !== 'undefined') {
      if (token) {
        localStorage.setItem('token', token);
      } else {
        localStorage.removeItem('token');
      }
    }
  }

  getToken(): string | null {
    if (typeof window !== 'undefined' && !this.token) {
      this.token = localStorage.getItem('token');
    }
    return this.token;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (this.token) {
      (headers as Record<string, string>)['Authorization'] = `Bearer ${this.token}`;
    }

    const response = await fetch(`${API_URL}${endpoint}`, {
      ...options,
      headers,
    });

    const data = await response.json();
    
    if (!response.ok) {
      throw new Error(data.message || 'An error occurred');
    }

    return data;
  }

  // Auth endpoints
  async register(name: string, email: string, password: string): Promise<AuthResponse> {
    const response = await this.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password }),
    });
    if (response.data) {
      this.setToken(response.data.token);
    }
    return response.data;
  }

  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    if (response.data) {
      this.setToken(response.data.token);
    }
    return response.data;
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.request<User>('/auth/me');
    return response.data;
  }

  logout() {
    this.setToken(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem('user');
    }
  }

  // GitHub endpoints
  async getGitHubLoginUrl(): Promise<string> {
    const response = await this.request<{ url: string }>('/github/login-url');
    return response.data.url;
  }

  async handleGitHubCallback(code: string): Promise<AuthResponse> {
    const response = await this.request<AuthResponse>('/github/callback', {
      method: 'POST',
      body: JSON.stringify({ code }),
    });
    if (response.data) {
      this.setToken(response.data.token);
    }
    return response.data;
  }

  async fetchRepositories(months: number = 6, includeForked: boolean = false): Promise<Repository[]> {
    const response = await this.request<Repository[]>(
      `/github/fetch-repos?months=${months}&includeForked=${includeForked}`,
      { method: 'POST' }
    );
    return response.data;
  }

  async getRepositories(): Promise<Repository[]> {
    const response = await this.request<Repository[]>('/github/repos');
    return response.data;
  }

  // Analysis endpoints
  async runAnalysis(request: AnalysisRequest): Promise<AnalysisResult> {
    const response = await this.request<AnalysisResult>('/analysis/run', {
      method: 'POST',
      body: JSON.stringify(request),
    });
    return response.data;
  }

  async getLatestAnalysis(): Promise<AnalysisResult | null> {
    const response = await this.request<AnalysisResult>('/analysis/latest');
    return response.data;
  }

  async getAnalysisHistory(): Promise<AnalysisResult[]> {
    const response = await this.request<AnalysisResult[]>('/analysis/history');
    return response.data;
  }

  async getAnalysisById(id: string): Promise<AnalysisResult> {
    const response = await this.request<AnalysisResult>(`/analysis/${id}`);
    return response.data;
  }
}

export const api = new ApiClient();
