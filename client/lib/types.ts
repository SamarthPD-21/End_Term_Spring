// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

// User Types
export interface User {
  id: string;
  email: string;
  name: string;
  githubUsername: string | null;
  avatarUrl: string | null;
  hasGithubLinked: boolean;
  lastAnalysisAt: string | null;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: User;
  expiresIn: number;
}

// Repository Types
export interface Repository {
  id: string;
  name: string;
  fullName: string;
  description: string;
  htmlUrl: string;
  language: string | null;
  topics: string[];
  stars: number;
  forks: number;
  processed: boolean;
  updatedAt: string;
}

// Pipeline Types - For tracking analysis stages
export interface PipelineStage {
  id: string;
  name: string;
  status: 'pending' | 'processing' | 'completed' | 'error';
  progress?: number;
  description?: string;
  icon?: string;
}

export interface RepoExtraction {
  repoId: string;
  repoName: string;
  languages: string[];
  frameworks: string[];
  tools: string[];
  skills: string[];
  complexity: 'beginner' | 'intermediate' | 'advanced';
}

export interface PipelineProgress {
  stages: PipelineStage[];
  currentStage: number;
  totalRepos: number;
  processedRepos: number;
  extractions: RepoExtraction[];
}

// Analysis Types
export interface Skill {
  name: string;
  category: 'language' | 'framework' | 'tool' | 'concept' | 'methodology';
  proficiencyScore: number;
  evidence: string;
  projectCount?: number;
  lastUsed?: string;
  trend?: 'improving' | 'stable' | 'declining';
}

export interface SkillAnalysis {
  strongSkills: Skill[];
  moderateSkills: Skill[];
  weakSkills: Skill[];
  missingSkills: string[];
  skillsByCategory: Record<string, Skill[]>;
  totalSkillsCount: number;
  totalSkillsIdentified?: number;
  confidenceScore?: number;
}

// Enhanced recommendation types
export interface LearningRecommendation {
  skill: string;
  reason: string;
  priority: number;
  resources: string[];
  estimatedTimeToLearn: string;
  category: 'learn_new' | 'improve_existing' | 'practice_more';
  relatedSkills: string[];
  difficultyLevel: 'beginner' | 'intermediate' | 'advanced';
  careerImpact: 'high' | 'medium' | 'low';
}

export interface SkillGap {
  skill: string;
  importance: number;
  reason: string;
  learningPath: string[];
}

export interface SkillToImprove {
  skill: string;
  currentLevel: number;
  targetLevel: number;
  reason: string;
  suggestedProjects: string[];
}

export interface EnhancedRecommendations {
  skillsToLearn: LearningRecommendation[];
  skillsToImprove: LearningRecommendation[];
  practiceMore: LearningRecommendation[];
  careerAdvice: string[];
  nextMilestone: string;
}

export interface DeveloperProfile {
  experienceLevel: string;
  primaryLanguages: string[];
  primaryFrameworks: string[];
  projectTypes: string[];
  specialization: string;
  skillDistribution: Record<string, number>;
  strengths: string[];
  areasForGrowth: string[];
  careerStage: 'entry' | 'junior' | 'mid' | 'senior' | 'lead';
}

export interface AnalysisResult {
  analysisId: string;
  skillAnalysis: SkillAnalysis;
  recommendations: LearningRecommendation[];
  enhancedRecommendations?: EnhancedRecommendations;
  developerProfile: DeveloperProfile;
  repositoriesAnalyzed: number;
  analyzedAt: string;
  pipelineSummary?: {
    totalLanguages: number;
    totalFrameworks: number;
    totalTools: number;
    processingTimeMs: number;
    totalProcessingTime?: string;
    reposProcessed?: number;
    skillsExtracted?: number;
    aiModelUsed?: string;
  };
}

export interface AnalysisRequest {
  monthsToAnalyze: number;
  includeForkedRepos: boolean;
  excludeRepos: string[];
  filterMode: 'TIME_BASED' | 'SELECTED_REPOS' | 'COMBINED';
  selectedRepoIds: string[];
}
