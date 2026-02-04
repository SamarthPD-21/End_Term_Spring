'use client';

import { motion } from 'framer-motion';
import { SkillProgression, ProgressionStatus, SkillDelta } from '@/lib/types';
import { 
  TrendingUp, 
  TrendingDown, 
  Minus, 
  Zap,
  Clock,
  Target,
  ArrowUp,
  ArrowDown,
  Sparkles,
  RefreshCw,
  ChevronRight
} from 'lucide-react';
import { useState } from 'react';

interface ProgressionCardProps {
  progression: SkillProgression | null;
  onRefresh?: () => void;
  isLoading?: boolean;
}

const statusConfig: Record<ProgressionStatus, { 
  color: string; 
  bg: string; 
  icon: React.ReactNode; 
  label: string;
  glow: string;
}> = {
  RAPIDLY_IMPROVING: { 
    color: 'text-green-600 dark:text-green-400', 
    bg: 'bg-green-100 dark:bg-green-900/30',
    icon: <TrendingUp className="text-green-500" size={20} />,
    label: 'Rapidly Improving',
    glow: 'shadow-green-500/20'
  },
  IMPROVING: { 
    color: 'text-emerald-600 dark:text-emerald-400', 
    bg: 'bg-emerald-100 dark:bg-emerald-900/30',
    icon: <TrendingUp className="text-emerald-500" size={20} />,
    label: 'Improving',
    glow: 'shadow-emerald-500/20'
  },
  SLIGHTLY_IMPROVING: { 
    color: 'text-teal-600 dark:text-teal-400', 
    bg: 'bg-teal-100 dark:bg-teal-900/30',
    icon: <ArrowUp className="text-teal-500" size={20} />,
    label: 'Slightly Improving',
    glow: 'shadow-teal-500/20'
  },
  STAGNATING: { 
    color: 'text-yellow-600 dark:text-yellow-400', 
    bg: 'bg-yellow-100 dark:bg-yellow-900/30',
    icon: <Minus className="text-yellow-500" size={20} />,
    label: 'Stagnating',
    glow: 'shadow-yellow-500/20'
  },
  SLIGHTLY_REGRESSING: { 
    color: 'text-orange-600 dark:text-orange-400', 
    bg: 'bg-orange-100 dark:bg-orange-900/30',
    icon: <ArrowDown className="text-orange-500" size={20} />,
    label: 'Slightly Regressing',
    glow: 'shadow-orange-500/20'
  },
  REGRESSING: { 
    color: 'text-red-600 dark:text-red-400', 
    bg: 'bg-red-100 dark:bg-red-900/30',
    icon: <TrendingDown className="text-red-500" size={20} />,
    label: 'Regressing',
    glow: 'shadow-red-500/20'
  },
};

function DeltaIndicator({ delta }: { delta: SkillDelta }) {
  const isPositive = delta.proficiencyDelta > 0 || delta.isNewSkill;
  const isNegative = delta.proficiencyDelta < 0 || delta.isAbandoned;
  
  return (
    <motion.div
      initial={{ opacity: 0, x: -10 }}
      animate={{ opacity: 1, x: 0 }}
      className={`flex items-center justify-between p-2 rounded-lg ${
        isPositive 
          ? 'bg-green-50 dark:bg-green-900/20' 
          : isNegative 
            ? 'bg-red-50 dark:bg-red-900/20' 
            : 'bg-gray-50 dark:bg-gray-800'
      }`}
    >
      <span className="text-sm font-medium text-gray-800 dark:text-gray-200">
        {delta.skillName}
      </span>
      <div className="flex items-center gap-2">
        {delta.isNewSkill && (
          <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 dark:bg-green-800 text-green-700 dark:text-green-300">
            NEW
          </span>
        )}
        {delta.proficiencyDelta !== 0 && (
          <span className={`text-sm font-medium ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
            {isPositive ? '+' : ''}{delta.proficiencyDelta}
          </span>
        )}
      </div>
    </motion.div>
  );
}

export function ProgressionCard({ progression, onRefresh, isLoading }: ProgressionCardProps) {
  const [showDetails, setShowDetails] = useState(false);
  
  if (!progression) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700"
      >
        <div className="text-center py-8">
          <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
            <TrendingUp className="text-gray-400" size={32} />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
            No Progression Data Yet
          </h3>
          <p className="text-gray-500 dark:text-gray-400 text-sm max-w-sm mx-auto">
            Run at least 2 analyses to see how your skills are progressing over time.
          </p>
        </div>
      </motion.div>
    );
  }

  const status = statusConfig[progression.summary.overallStatus] || statusConfig.STAGNATING;
  const progressScore = progression.summary.overallProgressScore;
  
  // Get top improvements and regressions
  const improvements = progression.skillDeltas
    .filter(d => d.proficiencyDelta > 0 || d.isNewSkill)
    .sort((a, b) => b.proficiencyDelta - a.proficiencyDelta)
    .slice(0, 5);
  
  const regressions = progression.skillDeltas
    .filter(d => d.proficiencyDelta < 0 || d.isAbandoned)
    .sort((a, b) => a.proficiencyDelta - b.proficiencyDelta)
    .slice(0, 3);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden"
    >
      {/* Header with gradient */}
      <div className={`p-6 ${status.bg}`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <motion.div
              animate={{ scale: [1, 1.1, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
              {status.icon}
            </motion.div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Skill Progression
              </h3>
              <p className={`text-sm ${status.color} font-medium`}>
                {status.label}
              </p>
            </div>
          </div>
          {onRefresh && (
            <button
              onClick={onRefresh}
              disabled={isLoading}
              className="p-2 rounded-lg bg-white/50 dark:bg-gray-700/50 hover:bg-white/80 dark:hover:bg-gray-700/80 transition-colors"
            >
              <RefreshCw size={18} className={isLoading ? 'animate-spin' : ''} />
            </button>
          )}
        </div>

        {/* Progress Score */}
        <div className="flex items-center gap-4">
          <div className="flex-1">
            <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${Math.min(100, Math.max(0, (progressScore + 100) / 2))}%` }}
                transition={{ duration: 1, delay: 0.3 }}
                className={`h-full rounded-full ${
                  progressScore > 0 
                    ? 'bg-gradient-to-r from-green-400 to-emerald-500' 
                    : 'bg-gradient-to-r from-red-400 to-orange-500'
                }`}
              />
            </div>
          </div>
          <span className={`text-2xl font-bold ${status.color}`}>
            {progressScore > 0 ? '+' : ''}{progressScore.toFixed(0)}
          </span>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-4 gap-4 p-6 border-b border-gray-100 dark:border-gray-700">
        <div className="text-center">
          <div className="flex items-center justify-center gap-1 text-green-600 mb-1">
            <ArrowUp size={16} />
            <span className="text-xl font-bold">{progression.summary.totalSkillsImproved}</span>
          </div>
          <p className="text-xs text-gray-500 dark:text-gray-400">Improved</p>
        </div>
        <div className="text-center">
          <div className="flex items-center justify-center gap-1 text-purple-600 mb-1">
            <Sparkles size={16} />
            <span className="text-xl font-bold">{progression.summary.newSkillsAcquired}</span>
          </div>
          <p className="text-xs text-gray-500 dark:text-gray-400">New Skills</p>
        </div>
        <div className="text-center">
          <div className="flex items-center justify-center gap-1 text-yellow-600 mb-1">
            <Minus size={16} />
            <span className="text-xl font-bold">{progression.summary.totalSkillsStagnant}</span>
          </div>
          <p className="text-xs text-gray-500 dark:text-gray-400">Stagnant</p>
        </div>
        <div className="text-center">
          <div className="flex items-center justify-center gap-1 text-red-600 mb-1">
            <ArrowDown size={16} />
            <span className="text-xl font-bold">{progression.summary.totalSkillsRegressed}</span>
          </div>
          <p className="text-xs text-gray-500 dark:text-gray-400">Regressed</p>
        </div>
      </div>

      {/* Velocity Section */}
      <div className="p-6 border-b border-gray-100 dark:border-gray-700">
        <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2">
          <Zap size={16} className="text-yellow-500" />
          Learning Velocity
        </h4>
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
            <p className="text-xs text-gray-500 dark:text-gray-400">New skills/month</p>
            <p className="text-lg font-semibold text-gray-900 dark:text-white">
              {progression.velocity.newSkillsPerMonth.toFixed(1)}
            </p>
          </div>
          <div className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
            <p className="text-xs text-gray-500 dark:text-gray-400">Momentum</p>
            <p className="text-lg font-semibold text-gray-900 dark:text-white capitalize">
              {progression.velocity.learningMomentum}
            </p>
          </div>
          <div className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
            <p className="text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1">
              <Clock size={12} />
              Days to next level
            </p>
            <p className="text-lg font-semibold text-gray-900 dark:text-white">
              {progression.velocity.estimatedDaysToNextLevel}
            </p>
          </div>
          <div className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
            <p className="text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1">
              <Target size={12} />
              Days to senior
            </p>
            <p className="text-lg font-semibold text-gray-900 dark:text-white">
              {progression.velocity.estimatedDaysToSenior}
            </p>
          </div>
        </div>
      </div>

      {/* Skill Changes */}
      <div className="p-6">
        <button
          onClick={() => setShowDetails(!showDetails)}
          className="w-full flex items-center justify-between text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3"
        >
          <span>Skill Changes</span>
          <ChevronRight 
            size={16} 
            className={`transform transition-transform ${showDetails ? 'rotate-90' : ''}`} 
          />
        </button>
        
        {showDetails && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="space-y-4"
          >
            {improvements.length > 0 && (
              <div>
                <h5 className="text-xs text-green-600 dark:text-green-400 font-medium mb-2 flex items-center gap-1">
                  <ArrowUp size={12} />
                  Top Improvements
                </h5>
                <div className="space-y-1">
                  {improvements.map((delta, idx) => (
                    <DeltaIndicator key={idx} delta={delta} />
                  ))}
                </div>
              </div>
            )}
            
            {regressions.length > 0 && (
              <div>
                <h5 className="text-xs text-red-600 dark:text-red-400 font-medium mb-2 flex items-center gap-1">
                  <ArrowDown size={12} />
                  Areas of Concern
                </h5>
                <div className="space-y-1">
                  {regressions.map((delta, idx) => (
                    <DeltaIndicator key={idx} delta={delta} />
                  ))}
                </div>
              </div>
            )}
          </motion.div>
        )}
      </div>

      {/* Narrative */}
      {progression.summary.narrativeSummary && (
        <div className="px-6 pb-6">
          <p className="text-sm text-gray-600 dark:text-gray-400 italic bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
            "{progression.summary.narrativeSummary}"
          </p>
        </div>
      )}
    </motion.div>
  );
}
