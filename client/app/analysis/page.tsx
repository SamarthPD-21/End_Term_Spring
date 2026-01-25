'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { AnalysisResult, AnalysisRequest, PipelineStage } from '@/lib/types';
import { AnalysisForm } from '@/components/AnalysisForm';
import { ProfileCard } from '@/components/ProfileCard';
import { SkillsDisplay } from '@/components/SkillsDisplay';
import { RecommendationsList } from '@/components/RecommendationsList';
import { 
  Github, 
  AlertCircle, 
  History,
  Loader2,
  CheckCircle,
  ArrowLeft,
  GitBranch,
  Cpu,
  Sparkles,
  Database,
  Zap,
  BarChart3,
  TrendingUp,
  Clock,
  Layers
} from 'lucide-react';
import Link from 'next/link';

// Pipeline stages for visualization
const pipelineStages: PipelineStage[] = [
  { id: 'fetch', name: 'Fetching Repos', status: 'pending', icon: 'download' },
  { id: 'extract', name: 'Extracting Skills', status: 'pending', icon: 'cpu' },
  { id: 'aggregate', name: 'Aggregating Data', status: 'pending', icon: 'layers' },
  { id: 'analyze', name: 'AI Analysis', status: 'pending', icon: 'sparkles' },
  { id: 'recommend', name: 'Generating Insights', status: 'pending', icon: 'lightbulb' },
];

function PipelineVisualization({ 
  stages, 
  isActive,
  currentStage 
}: { 
  stages: PipelineStage[]; 
  isActive: boolean;
  currentStage: number;
}) {
  const getIcon = (iconName: string, isCompleted: boolean, isCurrent: boolean) => {
    const color = isCompleted 
      ? 'text-green-500' 
      : isCurrent 
        ? 'text-purple-500' 
        : 'text-gray-400';
    const size = 18;
    
    switch (iconName) {
      case 'download': return <GitBranch size={size} className={color} />;
      case 'cpu': return <Cpu size={size} className={color} />;
      case 'layers': return <Layers size={size} className={color} />;
      case 'sparkles': return <Sparkles size={size} className={color} />;
      case 'lightbulb': return <Zap size={size} className={color} />;
      default: return <Database size={size} className={color} />;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-gradient-to-r from-purple-900/20 via-indigo-900/20 to-purple-900/20 rounded-xl p-4 mb-6 border border-purple-500/30"
    >
      <div className="flex items-center gap-2 mb-3">
        <motion.div
          animate={{ rotate: isActive ? 360 : 0 }}
          transition={{ duration: 2, repeat: isActive ? Infinity : 0, ease: 'linear' }}
        >
          <Cpu size={18} className="text-purple-400" />
        </motion.div>
        <span className="text-sm font-medium text-purple-300">
          {isActive ? 'Analysis Pipeline Active' : 'Pipeline Ready'}
        </span>
      </div>
      
      <div className="flex items-center justify-between gap-1">
        {stages.map((stage, idx) => {
          const isCompleted = idx < currentStage;
          const isCurrent = idx === currentStage && isActive;
          
          return (
            <div key={stage.id} className="flex items-center flex-1">
              <motion.div
                className={`relative flex flex-col items-center flex-1 ${
                  idx < stages.length - 1 ? 'pr-2' : ''
                }`}
              >
                <motion.div
                  initial={false}
                  animate={{
                    scale: isCurrent ? [1, 1.2, 1] : 1,
                    boxShadow: isCurrent 
                      ? '0 0 20px rgba(168, 85, 247, 0.5)' 
                      : '0 0 0px rgba(168, 85, 247, 0)',
                  }}
                  transition={{ duration: 0.5, repeat: isCurrent ? Infinity : 0 }}
                  className={`w-10 h-10 rounded-lg flex items-center justify-center transition-all ${
                    isCompleted 
                      ? 'bg-green-500/20 border-green-500/50' 
                      : isCurrent 
                        ? 'bg-purple-500/30 border-purple-500/50' 
                        : 'bg-gray-700/50 border-gray-600/50'
                  } border`}
                >
                  {isCompleted ? (
                    <CheckCircle size={18} className="text-green-500" />
                  ) : isCurrent ? (
                    <Loader2 size={18} className="text-purple-400 animate-spin" />
                  ) : (
                    getIcon(stage.icon || '', isCompleted, isCurrent)
                  )}
                </motion.div>
                <span className={`text-xs mt-1.5 text-center leading-tight ${
                  isCompleted ? 'text-green-400' : isCurrent ? 'text-purple-300' : 'text-gray-500'
                }`}>
                  {stage.name}
                </span>
              </motion.div>
              
              {idx < stages.length - 1 && (
                <motion.div 
                  className="h-0.5 flex-shrink-0 w-4 -mt-5"
                  initial={{ scaleX: 0 }}
                  animate={{ 
                    scaleX: isCompleted ? 1 : 0,
                    backgroundColor: isCompleted ? '#22c55e' : '#6b7280'
                  }}
                  transition={{ duration: 0.3 }}
                  style={{ backgroundColor: isCompleted ? '#22c55e' : '#374151' }}
                />
              )}
            </div>
          );
        })}
      </div>
    </motion.div>
  );
}

function AnalysisStats({ analysis }: { analysis: AnalysisResult }) {
  const stats = [
    { 
      label: 'Repositories', 
      value: analysis.repositoriesAnalyzed,
      icon: <GitBranch size={18} />,
      color: 'from-blue-500 to-cyan-500'
    },
    { 
      label: 'Skills Found', 
      value: analysis.skillAnalysis?.totalSkillsIdentified || 0,
      icon: <Sparkles size={18} />,
      color: 'from-purple-500 to-pink-500'
    },
    { 
      label: 'Recommendations', 
      value: analysis.recommendations?.length || 0,
      icon: <TrendingUp size={18} />,
      color: 'from-green-500 to-emerald-500'
    },
    { 
      label: 'Confidence', 
      value: `${Math.round((analysis.skillAnalysis?.confidenceScore || 0) * 100)}%`,
      icon: <BarChart3 size={18} />,
      color: 'from-orange-500 to-amber-500'
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      {stats.map((stat, idx) => (
        <motion.div
          key={stat.label}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: idx * 0.1 }}
          whileHover={{ scale: 1.02, y: -2 }}
          className="bg-white dark:bg-gray-800 rounded-xl p-4 border border-gray-200 dark:border-gray-700 card-hover"
        >
          <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${stat.color} flex items-center justify-center text-white mb-3`}>
            {stat.icon}
          </div>
          <motion.p 
            className="text-2xl font-bold text-gray-900 dark:text-white"
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: idx * 0.1 + 0.2, type: 'spring' }}
          >
            {stat.value}
          </motion.p>
          <p className="text-sm text-gray-500 dark:text-gray-400">{stat.label}</p>
        </motion.div>
      ))}
    </div>
  );
}

function TabButton({ 
  active, 
  onClick, 
  children,
  icon
}: { 
  active: boolean; 
  onClick: () => void; 
  children: React.ReactNode;
  icon: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-2 px-4 py-2.5 rounded-lg font-medium transition-all ${
        active 
          ? 'bg-purple-600 text-white shadow-lg shadow-purple-500/25' 
          : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
      }`}
    >
      {icon}
      {children}
    </button>
  );
}

export default function AnalysisPage() {
  const { user, isAuthenticated, isLoading: authLoading, loginWithGitHub } = useAuth();
  const router = useRouter();
  
  const [currentAnalysis, setCurrentAnalysis] = useState<AnalysisResult | null>(null);
  const [analysisHistory, setAnalysisHistory] = useState<AnalysisResult[]>([]);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'skills' | 'recommendations'>('skills');
  const [pipelineStage, setPipelineStage] = useState(0);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (isAuthenticated) {
      loadData();
    }
  }, [isAuthenticated]);

  // Simulate pipeline stages during analysis
  useEffect(() => {
    if (isAnalyzing) {
      const stageInterval = setInterval(() => {
        setPipelineStage(prev => {
          if (prev >= pipelineStages.length - 1) {
            return prev;
          }
          return prev + 1;
        });
      }, 2000);
      
      return () => clearInterval(stageInterval);
    } else {
      setPipelineStage(0);
    }
  }, [isAnalyzing]);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const history = await api.getAnalysisHistory();
      setAnalysisHistory(history);
      if (history.length > 0) {
        setCurrentAnalysis(history[0]);
      }
    } catch (err) {
      console.error('Failed to load analysis history:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRunAnalysis = async (request: AnalysisRequest) => {
    setIsAnalyzing(true);
    setError(null);
    setSuccessMessage(null);
    setPipelineStage(0);
    
    try {
      const result = await api.runAnalysis(request);
      setPipelineStage(pipelineStages.length);
      setCurrentAnalysis(result);
      setAnalysisHistory(prev => [result, ...prev]);
      setSuccessMessage('Analysis completed successfully!');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to run analysis');
    } finally {
      setIsAnalyzing(false);
    }
  };

  if (authLoading || isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <motion.div 
          className="text-center"
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
        >
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
          >
            <Loader2 className="w-12 h-12 text-purple-600 mx-auto mb-4" />
          </motion.div>
          <p className="text-gray-600 dark:text-gray-400">Loading your analysis...</p>
        </motion.div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  // Check if GitHub is linked
  if (!user?.hasGithubLinked) {
    return (
      <motion.div 
        className="max-w-2xl mx-auto px-4 py-16 text-center"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <div className="bg-white dark:bg-gray-800 rounded-xl p-12 shadow-lg">
          <motion.div
            animate={{ scale: [1, 1.1, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
          >
            <Github size={64} className="mx-auto mb-6 text-gray-400" />
          </motion.div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
            Connect Your GitHub Account
          </h2>
          <p className="text-gray-600 dark:text-gray-400 mb-8">
            To analyze your repositories and identify your skills, you need to connect your GitHub account first.
          </p>
          <motion.button
            onClick={loginWithGitHub}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            className="inline-flex items-center gap-2 bg-gray-900 text-white px-8 py-3 rounded-lg font-medium hover:bg-gray-800 transition-colors"
          >
            <Github size={20} />
            Connect GitHub
          </motion.button>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div 
      className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8"
      >
        <div>
          <Link 
            href="/dashboard" 
            className="inline-flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-purple-600 mb-2 transition-colors"
          >
            <ArrowLeft size={18} />
            Back to Dashboard
          </Link>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
            Skill Analysis
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Analyze your GitHub repositories to discover and track your skills
          </p>
        </div>
        
        {currentAnalysis && (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400"
          >
            <Clock size={16} />
            Last analyzed: {new Date(currentAnalysis.analyzedAt).toLocaleDateString('en-US', {
              month: 'short',
              day: 'numeric',
              year: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            })}
          </motion.div>
        )}
      </motion.div>

      {/* Pipeline Visualization */}
      <PipelineVisualization 
        stages={pipelineStages} 
        isActive={isAnalyzing}
        currentStage={pipelineStage}
      />

      <div className="grid lg:grid-cols-3 gap-8">
        {/* Sidebar - Analysis Form & History */}
        <div className="lg:col-span-1 space-y-6">
          {/* Analysis Form */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
          >
            <AnalysisForm onSubmit={handleRunAnalysis} isLoading={isAnalyzing} />
          </motion.div>

          {/* Success Message */}
          <AnimatePresence>
            {successMessage && (
              <motion.div
                initial={{ opacity: 0, y: -10, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -10, scale: 0.95 }}
                className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 rounded-lg p-4 flex items-center gap-3"
              >
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: 'spring', delay: 0.1 }}
                >
                  <CheckCircle className="text-green-600" size={20} />
                </motion.div>
                <p className="text-green-700 dark:text-green-400">{successMessage}</p>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Error Message */}
          <AnimatePresence>
            {error && (
              <motion.div
                initial={{ opacity: 0, y: -10, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -10, scale: 0.95 }}
                className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4 flex items-center gap-3"
              >
                <AlertCircle className="text-red-600" size={20} />
                <p className="text-red-700 dark:text-red-400">{error}</p>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Analysis History */}
          {analysisHistory.length > 0 && (
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
              className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
            >
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                <History size={18} className="text-purple-500" />
                Analysis History
              </h3>
              <div className="space-y-2">
                {analysisHistory.slice(0, 5).map((analysis, idx) => (
                  <motion.button
                    key={analysis.analysisId}
                    onClick={() => setCurrentAnalysis(analysis)}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: idx * 0.05 }}
                    whileHover={{ x: 4 }}
                    className={`w-full text-left p-3 rounded-lg transition-all ${
                      currentAnalysis?.analysisId === analysis.analysisId
                        ? 'bg-purple-100 dark:bg-purple-900/50 border-purple-300 dark:border-purple-700'
                        : 'bg-gray-50 dark:bg-gray-700/50 hover:bg-gray-100 dark:hover:bg-gray-700'
                    } border`}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white text-sm">
                          {new Date(analysis.analyzedAt).toLocaleDateString('en-US', {
                            month: 'short',
                            day: 'numeric',
                            year: 'numeric'
                          })}
                        </p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">
                          {analysis.repositoriesAnalyzed} repos • {analysis.skillAnalysis?.totalSkillsIdentified || 0} skills
                        </p>
                      </div>
                      {currentAnalysis?.analysisId === analysis.analysisId && (
                        <motion.div
                          initial={{ scale: 0 }}
                          animate={{ scale: 1 }}
                          className="w-2 h-2 rounded-full bg-purple-500"
                        />
                      )}
                    </div>
                  </motion.button>
                ))}
              </div>
            </motion.div>
          )}
        </div>

        {/* Main Content - Analysis Results */}
        <div className="lg:col-span-2 space-y-6">
          {currentAnalysis ? (
            <>
              {/* Stats Overview */}
              <AnalysisStats analysis={currentAnalysis} />

              {/* Profile Card */}
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
              >
                <ProfileCard 
                  profile={currentAnalysis.developerProfile} 
                  repositoriesAnalyzed={currentAnalysis.repositoriesAnalyzed} 
                />
              </motion.div>

              {/* Tab Navigation */}
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
                className="flex gap-2 bg-gray-100 dark:bg-gray-800/50 p-1.5 rounded-xl"
              >
                <TabButton 
                  active={activeTab === 'skills'} 
                  onClick={() => setActiveTab('skills')}
                  icon={<Sparkles size={16} />}
                >
                  Skills Analysis
                </TabButton>
                <TabButton 
                  active={activeTab === 'recommendations'} 
                  onClick={() => setActiveTab('recommendations')}
                  icon={<TrendingUp size={16} />}
                >
                  Learning Roadmap
                </TabButton>
              </motion.div>

              {/* Tab Content */}
              <AnimatePresence mode="wait">
                {activeTab === 'skills' ? (
                  <motion.div
                    key="skills"
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 20 }}
                    transition={{ duration: 0.2 }}
                    className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700"
                  >
                    <SkillsDisplay skillAnalysis={currentAnalysis.skillAnalysis} />
                  </motion.div>
                ) : (
                  <motion.div
                    key="recommendations"
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -20 }}
                    transition={{ duration: 0.2 }}
                    className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700"
                  >
                    <RecommendationsList 
                      recommendations={currentAnalysis.recommendations}
                      enhancedRecommendations={currentAnalysis.enhancedRecommendations}
                    />
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Pipeline Summary (if available) */}
              {currentAnalysis.pipelineSummary && (
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.4 }}
                  className="bg-gradient-to-r from-gray-900 to-gray-800 rounded-xl p-6 text-white"
                >
                  <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                    <Cpu size={18} />
                    Pipeline Summary
                  </h3>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div>
                      <p className="text-gray-400">Total Time</p>
                      <p className="font-semibold">{currentAnalysis.pipelineSummary.totalProcessingTime}</p>
                    </div>
                    <div>
                      <p className="text-gray-400">Repos Processed</p>
                      <p className="font-semibold">{currentAnalysis.pipelineSummary.reposProcessed}</p>
                    </div>
                    <div>
                      <p className="text-gray-400">Skills Extracted</p>
                      <p className="font-semibold">{currentAnalysis.pipelineSummary.skillsExtracted}</p>
                    </div>
                    <div>
                      <p className="text-gray-400">Analysis Mode</p>
                      <p className="font-semibold capitalize">{currentAnalysis.pipelineSummary.aiModelUsed || 'Local'}</p>
                    </div>
                  </div>
                </motion.div>
              )}
            </>
          ) : (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="bg-white dark:bg-gray-800 rounded-xl p-12 shadow-sm border border-gray-200 dark:border-gray-700 text-center"
            >
              <motion.div 
                className="text-6xl mb-4"
                animate={{ y: [0, -10, 0] }}
                transition={{ duration: 2, repeat: Infinity }}
              >
                🚀
              </motion.div>
              <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
                Ready to Analyze Your Skills?
              </h2>
              <p className="text-gray-600 dark:text-gray-400 max-w-md mx-auto">
                Configure your analysis settings on the left and click &quot;Run Analysis&quot; to get started.
                We&apos;ll analyze your GitHub repositories and provide personalized insights.
              </p>
            </motion.div>
          )}
        </div>
      </div>
    </motion.div>
  );
}
