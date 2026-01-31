'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { AnalysisResult, Repository } from '@/lib/types';
import { ProfileCard } from '@/components/ProfileCard';
import { SkillsDisplay } from '@/components/SkillsDisplay';
import { RecommendationsList } from '@/components/RecommendationsList';
import { 
  Github, 
  RefreshCw, 
  Clock, 
  FolderGit2, 
  AlertCircle,
  ArrowRight,
  Loader2
} from 'lucide-react';
import Link from 'next/link';

export default function DashboardPage() {
  const { user, isAuthenticated, isLoading: authLoading, loginWithGitHub } = useAuth();
  const router = useRouter();
  
  const [latestAnalysis, setLatestAnalysis] = useState<AnalysisResult | null>(null);
  const [repositories, setRepositories] = useState<Repository[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (isAuthenticated) {
      loadDashboardData();
    }
  }, [isAuthenticated]);

  const loadDashboardData = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const [analysisData, reposData] = await Promise.all([
        api.getLatestAnalysis().catch(() => null),
        api.getRepositories().catch(() => [])
      ]);
      
      setLatestAnalysis(analysisData);
      setRepositories(reposData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard data');
    } finally {
      setIsLoading(false);
    }
  };

  if (authLoading || isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin text-purple-600 mx-auto mb-4" />
          <p className="text-gray-600 dark:text-gray-400">Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
          Welcome back, {user?.name?.split(' ')[0]}! 👋
        </h1>
        <p className="text-gray-600 dark:text-gray-400">
          Track your skills and get personalized learning recommendations.
        </p>
      </div>

      {/* GitHub Connection Card */}
      {!user?.hasGithubLinked && (
        <div className="bg-gradient-to-r from-gray-900 to-gray-800 rounded-xl p-6 mb-8 text-white">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Github size={40} />
              <div>
                <h3 className="text-lg font-semibold">Connect Your GitHub</h3>
                <p className="text-gray-300">Link your GitHub account to analyze your repositories</p>
              </div>
            </div>
            <button
              onClick={loginWithGitHub}
              className="bg-white text-gray-900 px-6 py-2 rounded-lg font-medium hover:bg-gray-100 transition-colors flex items-center gap-2"
            >
              <Github size={18} />
              Connect GitHub
            </button>
          </div>
        </div>
      )}

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-purple-100 dark:bg-purple-900/50 rounded-lg flex items-center justify-center">
              <FolderGit2 className="text-purple-600" size={24} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">{repositories.length}</p>
              <p className="text-sm text-gray-600 dark:text-gray-400">Repositories</p>
            </div>
          </div>
        </div>
        
        <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-green-100 dark:bg-green-900/50 rounded-lg flex items-center justify-center">
              <Clock className="text-green-600" size={24} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                {latestAnalysis ? new Date(latestAnalysis.analyzedAt).toLocaleDateString() : 'Never'}
              </p>
              <p className="text-sm text-gray-600 dark:text-gray-400">Last Analysis</p>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-blue-100 dark:bg-blue-900/50 rounded-lg flex items-center justify-center">
              <RefreshCw className="text-blue-600" size={24} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                {latestAnalysis?.repositoriesAnalyzed || 0}
              </p>
              <p className="text-sm text-gray-600 dark:text-gray-400">Repos Analyzed</p>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-xl p-4 mb-8 flex items-center gap-3">
          <AlertCircle className="text-red-600" size={20} />
          <p className="text-red-700 dark:text-red-400">{error}</p>
        </div>
      )}

      {/* Main Content */}
      {latestAnalysis ? (
        <div className="space-y-8">
          {/* Profile Card */}
          <ProfileCard 
            profile={latestAnalysis.developerProfile} 
            repositoriesAnalyzed={latestAnalysis.repositoriesAnalyzed} 
          />

          {/* Skills Display */}
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700">
            <SkillsDisplay skillAnalysis={latestAnalysis.skillAnalysis} />
          </div>

          {/* Recommendations */}
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700">
            <RecommendationsList recommendations={latestAnalysis.recommendations} />
          </div>

          {/* Run New Analysis CTA */}
          <div className="text-center py-8">
            <Link
              href="/analysis"
              className="inline-flex items-center gap-2 bg-purple-600 text-white px-8 py-3 rounded-lg font-medium hover:bg-purple-700 transition-colors"
            >
              Run New Analysis
              <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      ) : (
        /* No Analysis Yet */
        <div className="bg-white dark:bg-gray-800 rounded-xl p-12 shadow-sm border border-gray-200 dark:border-gray-700 text-center">
          <div className="text-6xl mb-4">🔍</div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            No Analysis Yet
          </h2>
          <p className="text-gray-600 dark:text-gray-400 mb-6 max-w-md mx-auto">
            {user?.hasGithubLinked 
              ? "Run your first analysis to discover your skills and get personalized recommendations."
              : "Connect your GitHub account first, then run an analysis to discover your skills."}
          </p>
          {user?.hasGithubLinked ? (
            <Link
              href="/analysis"
              className="inline-flex items-center gap-2 bg-purple-600 text-white px-8 py-3 rounded-lg font-medium hover:bg-purple-700 transition-colors"
            >
              Start Your First Analysis
              <ArrowRight size={18} />
            </Link>
          ) : (
            <button
              onClick={loginWithGitHub}
              className="inline-flex items-center gap-2 bg-gray-900 text-white px-8 py-3 rounded-lg font-medium hover:bg-gray-800 transition-colors"
            >
              <Github size={18} />
              Connect GitHub
            </button>
          )}
        </div>
      )}
    </div>
  );
}
