'use client';

import { motion } from 'framer-motion';
import { RoleInference, RoleDistance } from '@/lib/types';
import { 
  Target, 
  Award,
  ChevronRight,
  Clock,
  CheckCircle,
  XCircle,
  Sparkles,
  RefreshCw,
  TrendingUp,
  Users
} from 'lucide-react';
import { useState } from 'react';

interface RoleInferenceCardProps {
  roleInference: RoleInference | null;
  onRefresh?: () => void;
  isLoading?: boolean;
}

const levelConfig: Record<string, { 
  color: string; 
  bg: string; 
  gradient: string;
  description: string;
}> = {
  junior: { 
    color: 'text-green-600 dark:text-green-400', 
    bg: 'bg-green-100 dark:bg-green-900/30',
    gradient: 'from-green-500 to-emerald-500',
    description: 'Building foundational skills'
  },
  mid: { 
    color: 'text-blue-600 dark:text-blue-400', 
    bg: 'bg-blue-100 dark:bg-blue-900/30',
    gradient: 'from-blue-500 to-cyan-500',
    description: 'Deepening expertise'
  },
  senior: { 
    color: 'text-purple-600 dark:text-purple-400', 
    bg: 'bg-purple-100 dark:bg-purple-900/30',
    gradient: 'from-purple-500 to-indigo-500',
    description: 'Leading & mentoring'
  },
};

function ProbabilityBar({ level, probability, isActive }: { 
  level: string; 
  probability: number; 
  isActive: boolean;
}) {
  const config = levelConfig[level] || levelConfig.junior;
  
  return (
    <div className="flex items-center gap-3">
      <span className={`text-sm font-medium w-16 capitalize ${isActive ? config.color : 'text-gray-500'}`}>
        {level}
      </span>
      <div className="flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${probability * 100}%` }}
          transition={{ duration: 0.8, delay: 0.2 }}
          className={`h-full rounded-full ${isActive ? `bg-gradient-to-r ${config.gradient}` : 'bg-gray-400'}`}
        />
      </div>
      <span className="text-sm font-medium text-gray-600 dark:text-gray-400 w-12 text-right">
        {(probability * 100).toFixed(0)}%
      </span>
    </div>
  );
}

function RoleDistanceCard({ role, index }: { role: RoleDistance; index: number }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const matchPercent = 100 - role.overallDistance;
  
  // Handle both backend and frontend field names
  const criticalSkillsRequired = role.criticalSkillsRequired ?? role.criticalSkillsTotal ?? 0;
  const missingCriticalSkills = role.missingCriticalSkills ?? role.topGaps ?? [];
  const roadmap = role.roadmap ?? [];
  
  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.1 }}
      className="bg-gray-50 dark:bg-gray-700/50 rounded-lg overflow-hidden"
    >
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full p-4 text-left"
      >
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <Target size={16} className="text-purple-500" />
            <span className="font-medium text-gray-900 dark:text-white">
              {role.roleName}
            </span>
            <span className="text-xs px-2 py-0.5 rounded-full bg-gray-200 dark:bg-gray-600 text-gray-600 dark:text-gray-300 capitalize">
              {role.roleLevel}
            </span>
          </div>
          <ChevronRight 
            size={16} 
            className={`text-gray-400 transform transition-transform ${isExpanded ? 'rotate-90' : ''}`}
          />
        </div>
        
        <div className="flex items-center gap-3">
          <div className="flex-1 h-2 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${matchPercent}%` }}
              transition={{ duration: 0.8 }}
              className={`h-full rounded-full ${
                matchPercent >= 70 ? 'bg-green-500' : 
                matchPercent >= 40 ? 'bg-yellow-500' : 'bg-red-500'
              }`}
            />
          </div>
          <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
            {matchPercent.toFixed(0)}% match
          </span>
        </div>
        
        <div className="flex items-center gap-4 mt-2 text-xs text-gray-500 dark:text-gray-400">
          <span className="flex items-center gap-1">
            <Clock size={12} />
            ~{role.estimatedWeeksToReach} weeks
          </span>
          <span className="flex items-center gap-1">
            <CheckCircle size={12} className="text-green-500" />
            {role.criticalSkillsMatched}/{criticalSkillsRequired} critical
          </span>
        </div>
      </button>
      
      {isExpanded && (
        <motion.div
          initial={{ height: 0, opacity: 0 }}
          animate={{ height: 'auto', opacity: 1 }}
          exit={{ height: 0, opacity: 0 }}
          className="px-4 pb-4 space-y-3"
        >
          {/* Missing Critical Skills */}
          {missingCriticalSkills.length > 0 && (
            <div>
              <h5 className="text-xs text-red-600 dark:text-red-400 font-medium mb-2 flex items-center gap-1">
                <XCircle size={12} />
                Missing Critical Skills
              </h5>
              <div className="flex flex-wrap gap-1">
                {missingCriticalSkills.map((skill, idx) => (
                  <span 
                    key={idx}
                    className="text-xs px-2 py-1 rounded-full bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400"
                  >
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          )}
          
          {/* Roadmap */}
          {roadmap.length > 0 && (
            <div>
              <h5 className="text-xs text-blue-600 dark:text-blue-400 font-medium mb-2 flex items-center gap-1">
                <TrendingUp size={12} />
                Roadmap
              </h5>
              <ol className="space-y-1">
                {roadmap.slice(0, 3).map((step, idx) => (
                  <li key={idx} className="text-xs text-gray-600 dark:text-gray-400 flex items-start gap-2">
                    <span className="w-4 h-4 rounded-full bg-blue-100 dark:bg-blue-900/50 text-blue-600 dark:text-blue-400 flex items-center justify-center text-xs flex-shrink-0">
                      {idx + 1}
                    </span>
                    {step}
                  </li>
                ))}
              </ol>
            </div>
          )}
        </motion.div>
      )}
    </motion.div>
  );
}

export function RoleInferenceCard({ roleInference, onRefresh, isLoading }: RoleInferenceCardProps) {
  const [showAllRoles, setShowAllRoles] = useState(false);
  
  if (!roleInference) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700"
      >
        <div className="text-center py-8">
          <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
            <Award className="text-gray-400" size={32} />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
            No Role Analysis Yet
          </h3>
          <p className="text-gray-500 dark:text-gray-400 text-sm max-w-sm mx-auto mb-4">
            Run a role inference to see your estimated level and distance to target roles.
          </p>
          {onRefresh && (
            <button
              onClick={onRefresh}
              disabled={isLoading}
              className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center gap-2 mx-auto"
            >
              {isLoading ? <RefreshCw size={16} className="animate-spin" /> : <Sparkles size={16} />}
              Analyze Role
            </button>
          )}
        </div>
      </motion.div>
    );
  }

  const level = roleInference.inferredLevel;
  const config = levelConfig[level.level] || levelConfig.junior;
  const sortedRoles = [...roleInference.roleDistances].sort((a, b) => a.overallDistance - b.overallDistance);
  const displayRoles = showAllRoles ? sortedRoles : sortedRoles.slice(0, 3);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden"
    >
      {/* Header */}
      <div className={`p-6 bg-gradient-to-r ${config.gradient} text-white`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <motion.div
              animate={{ rotate: [0, 10, -10, 0] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
              <Award size={28} />
            </motion.div>
            <div>
              <h3 className="text-lg font-semibold">Role Analysis</h3>
              <p className="text-white/80 text-sm">{config.description}</p>
            </div>
          </div>
          {onRefresh && (
            <button
              onClick={onRefresh}
              disabled={isLoading}
              className="p-2 rounded-lg bg-white/20 hover:bg-white/30 transition-colors"
            >
              <RefreshCw size={18} className={isLoading ? 'animate-spin' : ''} />
            </button>
          )}
        </div>

        {/* Inferred Level Badge */}
        <div className="flex items-center gap-4">
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', stiffness: 200 }}
            className="bg-white/20 backdrop-blur-sm rounded-xl p-4 flex-1"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-white/80 mb-1">Inferred Level</p>
                <p className="text-2xl font-bold capitalize">{level.level}</p>
              </div>
              <div className="text-right">
                <p className="text-sm text-white/80 mb-1">Confidence</p>
                <p className="text-2xl font-bold">{(level.confidence * 100).toFixed(0)}%</p>
              </div>
            </div>
          </motion.div>
        </div>
      </div>

      {/* Level Probabilities */}
      <div className="p-6 border-b border-gray-100 dark:border-gray-700">
        <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4 flex items-center gap-2">
          <Users size={16} className="text-blue-500" />
          Level Probability Distribution
        </h4>
        <div className="space-y-3">
          <ProbabilityBar 
            level="junior" 
            probability={level.juniorProbability} 
            isActive={level.level === 'junior'}
          />
          <ProbabilityBar 
            level="mid" 
            probability={level.midProbability} 
            isActive={level.level === 'mid'}
          />
          <ProbabilityBar 
            level="senior" 
            probability={level.seniorProbability} 
            isActive={level.level === 'senior'}
          />
        </div>
      </div>

      {/* Key Factors */}
      {level.keyFactors.length > 0 && (
        <div className="p-6 border-b border-gray-100 dark:border-gray-700">
          <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2">
            <Sparkles size={16} className="text-yellow-500" />
            Key Factors
          </h4>
          <div className="space-y-2">
            {level.keyFactors.slice(0, 4).map((factor, idx) => (
              <div key={idx} className="flex items-center justify-between text-sm">
                <span className="text-gray-600 dark:text-gray-400">{factor.factorName}</span>
                <div className="flex items-center gap-2">
                  <div className="w-20 h-1.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-purple-500 rounded-full"
                      style={{ width: `${Math.abs(factor.contribution) * 100}%` }}
                    />
                  </div>
                  <span className={`font-medium ${factor.contribution > 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {factor.contribution > 0 ? '+' : ''}{(factor.contribution * 100).toFixed(0)}%
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Role Distances */}
      <div className="p-6">
        <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2">
          <Target size={16} className="text-purple-500" />
          Distance to Target Roles
        </h4>
        <div className="space-y-2">
          {displayRoles.map((role, idx) => (
            <RoleDistanceCard key={role.roleId} role={role} index={idx} />
          ))}
        </div>
        
        {sortedRoles.length > 3 && (
          <button
            onClick={() => setShowAllRoles(!showAllRoles)}
            className="mt-4 text-sm text-purple-600 dark:text-purple-400 hover:underline"
          >
            {showAllRoles ? 'Show less' : `Show all ${sortedRoles.length} roles`}
          </button>
        )}
      </div>

      {/* Explanation */}
      {level.explanation && (
        <div className="px-6 pb-6">
          <p className="text-sm text-gray-600 dark:text-gray-400 italic bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
            "{level.explanation}"
          </p>
        </div>
      )}
    </motion.div>
  );
}
