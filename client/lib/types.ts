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

// Skill Profile Types
export interface DetectedSkill {
  skillId: string;
  normalizedName: string;
  category: string;
  proficiencyScore: number;
  evidenceStrength: number;
  projectCount: number;
  sources: string[];
}

export interface SkillProfile {
  id: string;
  userId: string;
  detectedSkills: DetectedSkill[];
  skillsByCategory: Record<string, DetectedSkill[]>;
  createdAt: string;
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

// ============================================
// PHASE-2: Intelligence Layer Types
// ============================================

// Skill Progression Types
export type ProgressionStatus = 
  | 'RAPIDLY_IMPROVING' 
  | 'IMPROVING' 
  | 'SLIGHTLY_IMPROVING' 
  | 'STAGNATING' 
  | 'SLIGHTLY_REGRESSING' 
  | 'REGRESSING';

export type DeltaType = 
  | 'NEW_SKILL'
  | 'SIGNIFICANT_GAIN'
  | 'MODERATE_GAIN'
  | 'SLIGHT_GAIN'
  | 'STABLE'
  | 'SLIGHT_DECLINE'
  | 'SIGNIFICANT_DECLINE'
  | 'ABANDONED';

export interface SkillDelta {
  skillName: string;
  normalizedName: string;
  category: string;
  previousProficiency: number | null;
  currentProficiency: number | null;
  proficiencyDelta: number;
  deltaType: DeltaType;
  insight: string;
  isNewSkill: boolean;
  isAbandoned: boolean;
}

export interface VelocityMetrics {
  skillsPerMonth: number;
  newSkillsPerMonth: number;
  averageImprovementRate: number;
  learningMomentum: 'accelerating' | 'steady' | 'decelerating' | 'stalled';
  estimatedDaysToNextLevel: number;
  estimatedDaysToSenior: number;
}

export interface TrendAnalysis {
  dominantCategory: string;
  categoryTrends: Record<string, number>;
  focusAreaShift: string;
  detectedPatterns: string[];
}

export interface ProgressionSummary {
  overallStatus: ProgressionStatus;
  totalSkillsImproved: number;
  totalSkillsStagnant: number;
  totalSkillsRegressed: number;
  newSkillsAcquired: number;
  skillsAbandoned: number;
  overallProgressScore: number;
  narrativeSummary: string;
}

export interface SkillProgression {
  id: string;
  userId: string;
  previousProfileId: string;
  currentProfileId: string;
  previousSnapshotDate: string;
  currentSnapshotDate: string;
  daysBetweenSnapshots: number;
  summary: ProgressionSummary;
  skillDeltas: SkillDelta[];
  velocity: VelocityMetrics;
  trends: TrendAnalysis;
  createdAt: string;
}

// Role Inference Types
export interface LevelFactor {
  factorName: string;
  value: number;
  weight: number;
  contribution: number;
  description: string;
}

export interface InferredLevel {
  level: 'junior' | 'mid' | 'senior';
  confidence: number;
  juniorProbability: number;
  midProbability: number;
  seniorProbability: number;
  keyFactors: LevelFactor[];
  explanation: string;
}

export interface RoleDistance {
  roleId: string;
  roleName: string;
  roleLevel: string;
  specialization: string;
  overallDistance: number;
  technicalDistance: number;
  experienceDistance: number;
  engineeringPracticesDistance: number;
  criticalSkillsMatched: number;
  criticalSkillsRequired?: number; // Frontend alias
  criticalSkillsTotal?: number; // Backend field
  importantSkillsMatched?: number;
  importantSkillsTotal?: number;
  missingCriticalSkills?: string[]; // Frontend alias
  topGaps?: string[]; // Backend field
  strengths?: string[];
  estimatedWeeksToReach: number;
  difficultyAssessment?: string;
  roadmap?: string[]; // May not exist in backend
  marketDemandScore?: number;
  marketInsight?: string;
}

export interface RoleInference {
  id: string;
  userId: string;
  skillProfileId: string;
  inferredLevel: InferredLevel;
  roleDistances: RoleDistance[];
  createdAt: string;
}

// Market Demand Types
export interface DemandMetrics {
  demandScore: number;
  growthRate: number;
  salaryMultiplier: number;
  jobPostingsCount: number;
  demandLevel: string; // Backend field name
  competitionLevel?: string; // Frontend alias
}

export interface TrendData {
  direction: 'rising' | 'stable' | 'declining';
  momentum: number;
  forecast?: string; // Backend field name
  sixMonthForecast?: number; // Frontend computed value
  historicalScores?: number[];
}

export interface RoleDemand {
  roleId: string;
  relevanceScore: number;
  isCritical: boolean;
}

export interface MarketDemand {
  id: string;
  skillId: string;
  skillName?: string; // Backend field name
  displayName?: string; // Frontend alias
  category: string;
  currentDemand?: DemandMetrics; // Backend field name
  metrics?: DemandMetrics; // Frontend alias
  trend: TrendData;
  roleDemands?: Record<string, RoleDemand>; // Backend returns Map
  lastUpdated: string;
}

// Intelligence Report Types
export interface IntelligenceStatus {
  hasProgressionData: boolean;
  hasRoleInference: boolean;
  progressionStatus?: ProgressionStatus;
  progressScore?: number;
  skillsImproved?: number;
  newSkillsAcquired?: number;
  inferredLevel?: string;
  levelConfidence?: number;
  closestRole?: string;
  distanceToClosest?: number;
  weeksToClosest?: number;
}

export interface IntelligenceReport {
  fullReport: string;
  progression?: SkillProgression;
  roleInference?: RoleInference;
  topDemandSkills: MarketDemand[];
  risingSkills: MarketDemand[];
}
