'use client';

import { motion } from 'framer-motion';
import { IntelligenceStatus } from '@/lib/types';
import { 
  Brain, 
  TrendingUp, 
  Target, 
  Sparkles,
  ArrowRight,
  Clock
} from 'lucide-react';
import Link from 'next/link';

interface IntelligenceStatusCardProps {
  status: IntelligenceStatus | null;
  compact?: boolean;
}

export function IntelligenceStatusCard({ status, compact = false }: IntelligenceStatusCardProps) {
  if (!status) {
    return null;
  }

  const hasData = status.hasProgressionData || status.hasRoleInference;

  if (compact) {
    return (
      <Link href="/intelligence">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          whileHover={{ scale: 1.02 }}
          className="bg-gradient-to-r from-purple-500 to-indigo-600 rounded-lg p-4 text-white cursor-pointer"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Brain size={20} />
              <div>
                <p className="font-medium text-sm">Intelligence</p>
                {hasData ? (
                  <p className="text-white/80 text-xs">
                    {status.inferredLevel && `${status.inferredLevel} level`}
                    {status.inferredLevel && status.progressionStatus && ' · '}
                    {status.progressionStatus && status.progressionStatus.replace(/_/g, ' ').toLowerCase()}
                  </p>
                ) : (
                  <p className="text-white/80 text-xs">View insights</p>
                )}
              </div>
            </div>
            <ArrowRight size={16} />
          </div>
        </motion.div>
      </Link>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden"
    >
      <div className="p-6 bg-gradient-to-r from-purple-600 via-indigo-600 to-blue-600 text-white">
        <div className="flex items-center gap-3 mb-2">
          <motion.div
            animate={{ rotate: [0, 10, -10, 0] }}
            transition={{ duration: 2, repeat: Infinity }}
          >
            <Brain size={24} />
          </motion.div>
          <h3 className="text-lg font-semibold">Intelligence Insights</h3>
        </div>
        <p className="text-white/80 text-sm">
          Your skill progression and career analysis
        </p>
      </div>

      <div className="p-6">
        {hasData ? (
          <div className="space-y-4">
            {/* Level & Confidence */}
            {status.inferredLevel && (
              <div className="flex items-center justify-between p-3 bg-purple-50 dark:bg-purple-900/30 rounded-lg">
                <div className="flex items-center gap-2">
                  <Sparkles size={16} className="text-purple-500" />
                  <span className="text-sm text-gray-600 dark:text-gray-400">Current Level</span>
                </div>
                <div className="text-right">
                  <span className="font-semibold text-purple-700 dark:text-purple-300 capitalize">
                    {status.inferredLevel}
                  </span>
                  {status.levelConfidence && (
                    <span className="text-xs text-gray-500 ml-2">
                      ({(status.levelConfidence * 100).toFixed(0)}% confident)
                    </span>
                  )}
                </div>
              </div>
            )}

            {/* Progression Status */}
            {status.progressionStatus && (
              <div className="flex items-center justify-between p-3 bg-green-50 dark:bg-green-900/30 rounded-lg">
                <div className="flex items-center gap-2">
                  <TrendingUp size={16} className="text-green-500" />
                  <span className="text-sm text-gray-600 dark:text-gray-400">Progress Status</span>
                </div>
                <span className="font-semibold text-green-700 dark:text-green-300">
                  {status.progressionStatus.replace(/_/g, ' ')}
                </span>
              </div>
            )}

            {/* Closest Role */}
            {status.closestRole && (
              <div className="flex items-center justify-between p-3 bg-blue-50 dark:bg-blue-900/30 rounded-lg">
                <div className="flex items-center gap-2">
                  <Target size={16} className="text-blue-500" />
                  <span className="text-sm text-gray-600 dark:text-gray-400">Closest Role</span>
                </div>
                <div className="text-right">
                  <span className="font-semibold text-blue-700 dark:text-blue-300">
                    {status.closestRole}
                  </span>
                  {status.weeksToClosest !== undefined && (
                    <div className="flex items-center gap-1 text-xs text-gray-500 mt-0.5">
                      <Clock size={10} />
                      ~{status.weeksToClosest} weeks
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Skills Progress */}
            {(status.skillsImproved !== undefined || status.newSkillsAcquired !== undefined) && (
              <div className="grid grid-cols-2 gap-3">
                {status.skillsImproved !== undefined && (
                  <div className="text-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                      {status.skillsImproved}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">Skills Improved</p>
                  </div>
                )}
                {status.newSkillsAcquired !== undefined && (
                  <div className="text-center p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                      {status.newSkillsAcquired}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">New Skills</p>
                  </div>
                )}
              </div>
            )}

            <Link
              href="/intelligence"
              className="block w-full text-center py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg font-medium transition-colors"
            >
              View Full Report
            </Link>
          </div>
        ) : (
          <div className="text-center py-4">
            <p className="text-gray-500 dark:text-gray-400 text-sm mb-4">
              Run multiple analyses to unlock progression tracking and role inference.
            </p>
            <Link
              href="/intelligence"
              className="inline-flex items-center gap-2 text-purple-600 dark:text-purple-400 hover:underline text-sm"
            >
              Learn more
              <ArrowRight size={14} />
            </Link>
          </div>
        )}
      </div>
    </motion.div>
  );
}
