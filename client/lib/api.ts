import { 
  ApiResponse, 
  AuthResponse, 
  User, 
  Repository, 
  AnalysisResult, 
  AnalysisRequest,
  SkillProfile,
  SkillProgression,
  RoleInference,
  RoleDistance,
  MarketDemand,
  IntelligenceStatus,
  IntelligenceReport
} from './types';

const API_URL = (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '');

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

  async getGitHubLinkUrl(): Promise<string> {
    const response = await this.request<{ url: string }>('/github/link-url');
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

  async linkGitHub(code: string): Promise<User> {
    const response = await this.request<User>('/github/link', {
      method: 'POST',
      body: JSON.stringify({ code }),
    });
    return response.data;
  }

  async unlinkGitHub(): Promise<User> {
    const response = await this.request<User>('/github/unlink', {
      method: 'POST',
    });
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

  async deleteAnalysis(id: string): Promise<void> {
    await this.request<void>(`/analysis/${id}`, {
      method: 'DELETE',
    });
  }

  async deleteAllAnalyses(): Promise<void> {
    await this.request<void>('/analysis/all', {
      method: 'DELETE',
    });
  }

  async deleteAccount(): Promise<void> {
    await this.request<void>('/auth/account', {
      method: 'DELETE',
    });
    this.logout();
  }

  // ============================================
  // Phase-2: Intelligence Layer Endpoints
  // ============================================

  // Skill Profile endpoints
  async getLatestSkillProfile(): Promise<SkillProfile | null> {
    try {
      const response = await this.request<SkillProfile>('/skills/profile');
      // SkillProfileController returns object directly, not wrapped in ApiResponse
      return response.data || response;
    } catch {
      return null;
    }
  }

  async getSkillProfileHistory(): Promise<SkillProfile[]> {
    try {
      const response = await this.request<SkillProfile[]>('/skills/profile/history');
      // SkillProfileController returns array directly, not wrapped in ApiResponse
      return Array.isArray(response) ? response : (response.data || []);
    } catch {
      return [];
    }
  }

  // Progression endpoints
  async getLatestProgression(): Promise<SkillProgression | null> {
    try {
      const response = await this.request<SkillProgression | { available: false }>('/intelligence/progression');
      // IntelligenceController returns object directly, not wrapped in ApiResponse
      const data = response.data || response;
      if ('available' in data && (data as { available: boolean }).available === false) {
        return null;
      }
      return data as SkillProgression;
    } catch {
      return null;
    }
  }

  async computeProgression(): Promise<SkillProgression | null> {
    try {
      const response = await this.request<SkillProgression>('/intelligence/progression/compute', {
        method: 'POST',
      });
      // IntelligenceController returns object directly
      return response.data || response as unknown as SkillProgression;
    } catch {
      return null;
    }
  }

  async getProgressionHistory(): Promise<SkillProgression[]> {
    const response = await this.request<SkillProgression[]>('/intelligence/progression/history');
    return Array.isArray(response) ? response : (response.data || []);
  }

  async getProgressionNarrative(): Promise<string> {
    const response = await this.request<{ narrative: string }>('/intelligence/progression/narrative');
    const data = response.data || response;
    return (data as { narrative: string }).narrative;
  }

  // Role inference endpoints
  async inferRole(): Promise<RoleInference> {
    const response = await this.request<RoleInference>('/intelligence/role/infer', {
      method: 'POST',
    });
    return response.data || response as unknown as RoleInference;
  }

  async getLatestRoleInference(): Promise<RoleInference | null> {
    try {
      const response = await this.request<RoleInference | { available: false }>('/intelligence/role');
      const data = response.data || response;
      if ('available' in data && (data as { available: boolean }).available === false) {
        return null;
      }
      return data as RoleInference;
    } catch {
      return null;
    }
  }

  async getRoleInferenceHistory(): Promise<RoleInference[]> {
    const response = await this.request<RoleInference[]>('/intelligence/role/history');
    return Array.isArray(response) ? response : (response.data || []);
  }

  async getDistanceToRole(roleId: string): Promise<RoleDistance | null> {
    try {
      const response = await this.request<RoleDistance>(`/intelligence/role/distance/${roleId}`);
      return response.data || response as unknown as RoleDistance;
    } catch {
      return null;
    }
  }

  async getClosestRole(): Promise<RoleDistance | null> {
    try {
      const response = await this.request<RoleDistance | { available: false }>('/intelligence/role/closest');
      const data = response.data || response;
      if ('available' in data && (data as { available: boolean }).available === false) {
        return null;
      }
      return data as RoleDistance;
    } catch {
      return null;
    }
  }

  async getRoleNarrative(): Promise<string> {
    const response = await this.request<{ narrative: string }>('/intelligence/role/narrative');
    const data = response.data || response;
    return (data as { narrative: string }).narrative;
  }

  // Market demand endpoints
  async getSkillMarketDemand(skillId: string): Promise<MarketDemand | null> {
    try {
      const response = await this.request<MarketDemand | { message: string }>(`/intelligence/market/skill/${skillId}`);
      const data = response.data || response;
      if ('message' in data) {
        return null;
      }
      return data as MarketDemand;
    } catch {
      return null;
    }
  }

  async getTopDemandSkills(limit: number = 10): Promise<MarketDemand[]> {
    const response = await this.request<MarketDemand[]>(`/intelligence/market/top?limit=${limit}`);
    return Array.isArray(response) ? response : (response.data || []);
  }

  async getRisingSkills(limit: number = 10): Promise<MarketDemand[]> {
    const response = await this.request<MarketDemand[]>(`/intelligence/market/rising?limit=${limit}`);
    return Array.isArray(response) ? response : (response.data || []);
  }

  async getMarketSkillsByCategory(category: string): Promise<MarketDemand[]> {
    const response = await this.request<MarketDemand[]>(`/intelligence/market/category/${category}`);
    return Array.isArray(response) ? response : (response.data || []);
  }

  // Intelligence explanation endpoint
  async explainWhySkillMatters(skillId: string): Promise<string> {
    const response = await this.request<{ explanation: string }>(`/intelligence/explain/${skillId}`);
    const data = response.data || response;
    return (data as { explanation: string }).explanation;
  }

  // Comprehensive report endpoints
  async getIntelligenceReport(): Promise<IntelligenceReport> {
    const response = await this.request<IntelligenceReport>('/intelligence/report');
    return response.data || response as unknown as IntelligenceReport;
  }

  async getIntelligenceStatus(): Promise<IntelligenceStatus> {
    const response = await this.request<IntelligenceStatus>('/intelligence/status');
    return response.data || response as unknown as IntelligenceStatus;
  }
}

export const api = new ApiClient();
