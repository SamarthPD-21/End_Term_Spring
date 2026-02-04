'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { 
  SkillProfile,
  SkillProgression, 
  RoleInference, 
  MarketDemand,
  IntelligenceStatus 
} from '@/lib/types';
import { ProgressionCard } from '@/components/ProgressionCard';
import { RoleInferenceCard } from '@/components/RoleInferenceCard';
import { MarketInsights } from '@/components/MarketInsights';
import { 
  Brain, 
  RefreshCw, 
  Loader2,
  AlertCircle,
  Sparkles,
  TrendingUp,
  Target,
  Flame,
  ArrowLeft,
  FileText,
  X
} from 'lucide-react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'framer-motion';

export default function IntelligencePage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [skillProfiles, setSkillProfiles] = useState<SkillProfile[]>([]);
  const [progression, setProgression] = useState<SkillProgression | null>(null);
  const [roleInference, setRoleInference] = useState<RoleInference | null>(null);
  const [topDemandSkills, setTopDemandSkills] = useState<MarketDemand[]>([]);
  const [risingSkills, setRisingSkills] = useState<MarketDemand[]>([]);
  const [status, setStatus] = useState<IntelligenceStatus | null>(null);
  
  const [isRefreshingProgression, setIsRefreshingProgression] = useState(false);
  const [isRefreshingRole, setIsRefreshingRole] = useState(false);
  const [hasSkillProfile, setHasSkillProfile] = useState(true);
  const [isAutoComputing, setIsAutoComputing] = useState(false);
  
  const [skillExplanation, setSkillExplanation] = useState<{ skillId: string; explanation: string } | null>(null);
  const [isExplaining, setIsExplaining] = useState(false);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [authLoading, isAuthenticated, router]);

  const loadIntelligenceData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      // First check if user has any skill profiles
      const profileHistory = await api.getSkillProfileHistory().catch(() => []);
      setSkillProfiles(profileHistory || []);
      
      if (!profileHistory || profileHistory.length === 0) {
        setHasSkillProfile(false);
        // Still fetch market data even without profile
        const [topDemandData, risingData] = await Promise.all([
          api.getTopDemandSkills(8).catch(() => []),
          api.getRisingSkills(8).catch(() => [])
        ]);
        setTopDemandSkills(topDemandData);
        setRisingSkills(risingData);
        setIsLoading(false);
        return;
      }
      
      setHasSkillProfile(true);
      
      // Fetch existing data
      const [
        statusData,
        progressionData,
        roleData,
        topDemandData,
        risingData
      ] = await Promise.all([
        api.getIntelligenceStatus().catch(() => null),
        api.getLatestProgression().catch(() => null),
        api.getLatestRoleInference().catch(() => null),
        api.getTopDemandSkills(8).catch(() => []),
        api.getRisingSkills(8).catch(() => [])
      ]);
      
      setStatus(statusData);
      setProgression(progressionData);
      setRoleInference(roleData);
      setTopDemandSkills(topDemandData);
      setRisingSkills(risingData);
      
      // Auto-compute if we have profiles but no intelligence data
      setIsAutoComputing(true);
      
      const profileCount = profileHistory?.length || 0;
      
      // If we have 1+ profile but no role inference, compute it
      if (!roleData && profileCount >= 1) {
        try {
          const newRole = await api.inferRole();
          setRoleInference(newRole);
        } catch (e) {
          console.log('Auto role inference not available:', e);
        }
      }
      
      // If we have 2+ profiles but no progression, compute it
      if (!progressionData && profileCount >= 2) {
        try {
          const newProgression = await api.computeProgression();
          if (newProgression) {
            setProgression(newProgression);
          }
        } catch (e) {
          console.log('Auto progression compute not available:', e);
        }
      }
      
      // Refresh status after auto-computing
      if ((!roleData && profileCount >= 1) || (!progressionData && profileCount >= 2)) {
        const newStatus = await api.getIntelligenceStatus().catch(() => null);
        setStatus(newStatus);
      }
      
      setIsAutoComputing(false);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load intelligence data';
      setError(errorMessage);
      setIsAutoComputing(false);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      loadIntelligenceData();
    }
  }, [isAuthenticated, loadIntelligenceData]);

  const handleRefreshProgression = async () => {
    if (!hasSkillProfile) {
      setError('Please run an analysis first to generate your skill profile.');
      return;
    }
    setIsRefreshingProgression(true);
    try {
      const newProgression = await api.computeProgression();
      if (newProgression) {
        setProgression(newProgression);
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to refresh progression';
      if (message.includes('No skill profile') || message.includes('Need at least 2')) {
        setError('Need at least 2 skill analyses to track progression.');
      } else {
        console.error('Failed to refresh progression:', err);
      }
    } finally {
      setIsRefreshingProgression(false);
    }
  };

  const handleRefreshRole = async () => {
    if (!hasSkillProfile) {
      setError('Please run an analysis first to generate your skill profile.');
      return;
    }
    setIsRefreshingRole(true);
    try {
      const newRole = await api.inferRole();
      setRoleInference(newRole);
      setHasSkillProfile(true);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to refresh role';
      if (message.includes('No skill profile')) {
        setHasSkillProfile(false);
        setError('Please run an analysis first to generate your skill profile.');
      } else {
        console.error('Failed to refresh role:', err);
      }
    } finally {
      setIsRefreshingRole(false);
    }
  };

  const handleExplainSkill = async (skillId: string) => {
    setIsExplaining(true);
    try {
      const explanation = await api.explainWhySkillMatters(skillId);
      setSkillExplanation({ skillId, explanation });
    } catch (err) {
      console.error('Failed to explain skill:', err);
    } finally {
      setIsExplaining(false);
    }
  };

  if (authLoading || isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin text-purple-600 mx-auto mb-4" />
          <p className="text-gray-600 dark:text-gray-400">
            {isAutoComputing ? 'Computing intelligence insights...' : 'Loading intelligence data...'}
          </p>
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
        <Link 
          href="/dashboard"
          className="inline-flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-4"
        >
          <ArrowLeft size={16} />
          Back to Dashboard
        </Link>
        
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <motion.div
              animate={{ rotate: [0, 10, -10, 0] }}
              transition={{ duration: 2, repeat: Infinity }}
              className="p-3 bg-gradient-to-br from-purple-500 to-indigo-600 rounded-xl text-white"
            >
              <Brain size={28} />
            </motion.div>
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
                Intelligence Layer
              </h1>
              <p className="text-gray-600 dark:text-gray-400">
                Track your progression, understand your role, and discover market opportunities
              </p>
            </div>
          </div>
          
          <button
            onClick={loadIntelligenceData}
            className="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            <RefreshCw size={20} className={isLoading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Quick Status Bar */}
      {status && (status.hasProgressionData || status.hasRoleInference) && (
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-gradient-to-r from-purple-600 via-indigo-600 to-blue-600 rounded-xl p-4 mb-8 text-white"
        >
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {status.inferredLevel && (
              <div className="text-center">
                <p className="text-white/70 text-xs mb-1">Current Level</p>
                <p className="text-lg font-bold capitalize">{status.inferredLevel}</p>
              </div>
            )}
            {status.progressionStatus && (
              <div className="text-center">
                <p className="text-white/70 text-xs mb-1">Progress Status</p>
                <p className="text-lg font-bold">{status.progressionStatus.replace('_', ' ')}</p>
              </div>
            )}
            {status.closestRole && (
              <div className="text-center">
                <p className="text-white/70 text-xs mb-1">Closest Role</p>
                <p className="text-lg font-bold">{status.closestRole}</p>
              </div>
            )}
            {status.weeksToClosest !== undefined && (
              <div className="text-center">
                <p className="text-white/70 text-xs mb-1">Weeks to Reach</p>
                <p className="text-lg font-bold">{status.weeksToClosest}</p>
              </div>
            )}
          </div>
        </motion.div>
      )}

      {error && (
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-xl p-4 mb-8 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertCircle className="text-red-600" size={20} />
            <p className="text-red-700 dark:text-red-400">{error}</p>
          </div>
          <button 
            onClick={() => setError(null)}
            className="text-red-500 hover:text-red-700"
          >
            <X size={18} />
          </button>
        </div>
      )}

      {/* No Skill Profile Banner */}
      {!hasSkillProfile && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-800 rounded-xl p-6 mb-8"
        >
          <div className="flex items-start gap-4">
            <div className="p-3 bg-amber-100 dark:bg-amber-800 rounded-xl">
              <AlertCircle className="text-amber-600 dark:text-amber-400" size={24} />
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-amber-800 dark:text-amber-200 mb-2">
                No Skill Profile Found
              </h3>
              <p className="text-amber-700 dark:text-amber-300 text-sm mb-4">
                To unlock the full Intelligence Layer features (progression tracking, role inference, and personalized insights), 
                you need to run at least one skill analysis first.
              </p>
              <Link
                href="/analysis"
                className="inline-flex items-center gap-2 bg-amber-600 hover:bg-amber-700 text-white px-4 py-2 rounded-lg font-medium transition-colors"
              >
                <Sparkles size={16} />
                Run Your First Analysis
              </Link>
            </div>
          </div>
        </motion.div>
      )}

      {/* Single Profile Info Banner */}
      {hasSkillProfile && skillProfiles && skillProfiles.length === 1 && !progression && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800 rounded-xl p-4 mb-8"
        >
          <div className="flex items-center gap-3">
            <TrendingUp className="text-blue-600 dark:text-blue-400" size={20} />
            <div className="flex-1">
              <p className="text-blue-700 dark:text-blue-300 text-sm">
                <strong>Tip:</strong> Run another analysis after working on more projects to unlock <strong>Skill Progression Tracking</strong> 
                — compare your growth over time!
              </p>
            </div>
            <Link
              href="/analysis"
              className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-200 text-sm font-medium"
            >
              Run Analysis →
            </Link>
          </div>
        </motion.div>
      )}

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Left Column */}
        <div className="space-y-8">
          {/* Progression Card */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <TrendingUp className="text-green-500" size={20} />
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                Skill Progression
              </h2>
            </div>
            <ProgressionCard 
              progression={progression}
              onRefresh={handleRefreshProgression}
              isLoading={isRefreshingProgression}
            />
          </div>

          {/* Market Insights */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <Flame className="text-orange-500" size={20} />
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                Market Trends
              </h2>
            </div>
            <MarketInsights 
              topDemandSkills={topDemandSkills}
              risingSkills={risingSkills}
              onExplainSkill={handleExplainSkill}
            />
          </div>
        </div>

        {/* Right Column */}
        <div className="space-y-8">
          {/* Role Inference Card */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <Target className="text-purple-500" size={20} />
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                Role Analysis
              </h2>
            </div>
            <RoleInferenceCard 
              roleInference={roleInference}
              onRefresh={handleRefreshRole}
              isLoading={isRefreshingRole}
            />
          </div>

          {/* Quick Actions */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700"
          >
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <Sparkles className="text-yellow-500" size={20} />
              Quick Actions
            </h3>
            <div className="space-y-3">
              <Link
                href="/analysis"
                className="w-full flex items-center justify-between p-3 bg-purple-50 dark:bg-purple-900/30 rounded-lg hover:bg-purple-100 dark:hover:bg-purple-900/50 transition-colors"
              >
                <span className="text-purple-700 dark:text-purple-300 font-medium">
                  Run New Analysis
                </span>
                <RefreshCw size={16} className="text-purple-600" />
              </Link>
              <button
                onClick={handleRefreshProgression}
                disabled={isRefreshingProgression}
                className="w-full flex items-center justify-between p-3 bg-green-50 dark:bg-green-900/30 rounded-lg hover:bg-green-100 dark:hover:bg-green-900/50 transition-colors"
              >
                <span className="text-green-700 dark:text-green-300 font-medium">
                  Compute Progression
                </span>
                {isRefreshingProgression ? (
                  <Loader2 size={16} className="text-green-600 animate-spin" />
                ) : (
                  <TrendingUp size={16} className="text-green-600" />
                )}
              </button>
              <button
                onClick={handleRefreshRole}
                disabled={isRefreshingRole}
                className="w-full flex items-center justify-between p-3 bg-blue-50 dark:bg-blue-900/30 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-900/50 transition-colors"
              >
                <span className="text-blue-700 dark:text-blue-300 font-medium">
                  Infer Current Role
                </span>
                {isRefreshingRole ? (
                  <Loader2 size={16} className="text-blue-600 animate-spin" />
                ) : (
                  <Target size={16} className="text-blue-600" />
                )}
              </button>
            </div>
          </motion.div>
        </div>
      </div>

      {/* Skill Explanation Modal */}
      <AnimatePresence>
        {(skillExplanation || isExplaining) && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setSkillExplanation(null)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-lg w-full shadow-xl"
              onClick={e => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                  <FileText size={20} className="text-purple-500" />
                  Why This Skill Matters
                </h3>
                <button
                  onClick={() => setSkillExplanation(null)}
                  className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
                >
                  <X size={20} className="text-gray-500" />
                </button>
              </div>
              
              {isExplaining ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="w-8 h-8 animate-spin text-purple-600" />
                </div>
              ) : skillExplanation ? (
                <div className="prose dark:prose-invert max-w-none">
                  <p className="text-gray-600 dark:text-gray-400 whitespace-pre-wrap">
                    {skillExplanation.explanation}
                  </p>
                </div>
              ) : null}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
