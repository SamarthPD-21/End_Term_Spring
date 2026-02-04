'use client';

import { motion } from 'framer-motion';
import { MarketDemand } from '@/lib/types';
import { 
  TrendingUp, 
  TrendingDown,
  Minus,
  Flame,
  Briefcase,
  DollarSign,
  BarChart3,
  ChevronRight,
  Sparkles,
  Zap
} from 'lucide-react';
import { useState } from 'react';

interface MarketInsightsProps {
  topDemandSkills: MarketDemand[];
  risingSkills: MarketDemand[];
  onExplainSkill?: (skillId: string) => void;
}

const trendIcons: Record<string, React.ReactNode> = {
  rising: <TrendingUp size={14} className="text-green-500" />,
  stable: <Minus size={14} className="text-yellow-500" />,
  declining: <TrendingDown size={14} className="text-red-500" />,
};

function SkillDemandCard({ 
  skill, 
  index, 
  showGrowth = false,
  onExplain
}: { 
  skill: MarketDemand; 
  index: number;
  showGrowth?: boolean;
  onExplain?: (skillId: string) => void;
}) {
  const [isExpanded, setIsExpanded] = useState(false);
  
  // Handle both backend field names (currentDemand, skillName) and frontend aliases (metrics, displayName)
  const metrics = skill.metrics || skill.currentDemand;
  const displayName = skill.displayName || skill.skillName || skill.skillId;
  const demandScore = metrics?.demandScore ?? 0;
  const growthRate = metrics?.growthRate ?? 0;
  const jobPostingsCount = metrics?.jobPostingsCount ?? 0;
  const salaryMultiplier = metrics?.salaryMultiplier ?? 1;
  const competitionLevel = metrics?.competitionLevel || metrics?.demandLevel || 'moderate';
  const sixMonthForecast = skill.trend?.sixMonthForecast ?? (skill.trend?.momentum ? skill.trend.momentum * 10 : 0);
  
  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.05 }}
      whileHover={{ scale: 1.01 }}
      className="bg-gray-50 dark:bg-gray-700/50 rounded-lg overflow-hidden cursor-pointer"
      onClick={() => setIsExpanded(!isExpanded)}
    >
      <div className="p-4">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <span className="w-6 h-6 rounded-full bg-purple-100 dark:bg-purple-900/50 text-purple-600 dark:text-purple-400 flex items-center justify-center text-xs font-bold">
              {index + 1}
            </span>
            <span className="font-medium text-gray-900 dark:text-white">
              {displayName}
            </span>
            {trendIcons[skill.trend?.direction || 'stable']}
          </div>
          <div className="flex items-center gap-2">
            {showGrowth && (
              <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                growthRate > 10 
                  ? 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400'
                  : 'bg-gray-100 text-gray-700 dark:bg-gray-600 dark:text-gray-300'
              }`}>
                +{growthRate.toFixed(0)}%
              </span>
            )}
            <ChevronRight 
              size={16} 
              className={`text-gray-400 transform transition-transform ${isExpanded ? 'rotate-90' : ''}`}
            />
          </div>
        </div>
        
        <div className="flex items-center gap-2">
          <div className="flex-1 h-2 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${demandScore}%` }}
              transition={{ duration: 0.8, delay: index * 0.05 }}
              className={`h-full rounded-full ${
                demandScore >= 80 ? 'bg-gradient-to-r from-purple-500 to-pink-500' :
                demandScore >= 60 ? 'bg-gradient-to-r from-blue-500 to-cyan-500' :
                'bg-gradient-to-r from-gray-400 to-gray-500'
              }`}
            />
          </div>
          <span className="text-sm font-medium text-gray-600 dark:text-gray-400 w-10 text-right">
            {demandScore}
          </span>
        </div>
      </div>
      
      {isExpanded && (
        <motion.div
          initial={{ height: 0, opacity: 0 }}
          animate={{ height: 'auto', opacity: 1 }}
          exit={{ height: 0, opacity: 0 }}
          className="px-4 pb-4 border-t border-gray-100 dark:border-gray-600 pt-3"
        >
          <div className="grid grid-cols-3 gap-3 mb-3">
            <div className="text-center">
              <div className="flex items-center justify-center gap-1 text-blue-600 mb-1">
                <Briefcase size={14} />
              </div>
              <p className="text-lg font-semibold text-gray-900 dark:text-white">
                {(jobPostingsCount / 1000).toFixed(0)}k
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Job Posts</p>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center gap-1 text-green-600 mb-1">
                <DollarSign size={14} />
              </div>
              <p className="text-lg font-semibold text-gray-900 dark:text-white">
                {salaryMultiplier.toFixed(1)}x
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Salary Mult</p>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center gap-1 text-yellow-600 mb-1">
                <BarChart3 size={14} />
              </div>
              <p className="text-lg font-semibold text-gray-900 dark:text-white capitalize">
                {competitionLevel.replace('_', ' ')}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Competition</p>
            </div>
          </div>
          
          <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 mb-3">
            <span>6-month forecast: {sixMonthForecast > 0 ? '+' : ''}{sixMonthForecast.toFixed(0)}%</span>
            <span className="capitalize">{skill.category}</span>
          </div>
          
          {onExplain && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onExplain(skill.skillId);
              }}
              className="w-full text-sm text-purple-600 dark:text-purple-400 hover:bg-purple-50 dark:hover:bg-purple-900/30 rounded-lg p-2 flex items-center justify-center gap-1"
            >
              <Sparkles size={14} />
              Why does this matter for me?
            </button>
          )}
        </motion.div>
      )}
    </motion.div>
  );
}

export function MarketInsights({ topDemandSkills, risingSkills, onExplainSkill }: MarketInsightsProps) {
  const [activeTab, setActiveTab] = useState<'demand' | 'rising'>('demand');
  
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden"
    >
      {/* Header */}
      <div className="p-6 bg-gradient-to-r from-orange-500 to-pink-500 text-white">
        <div className="flex items-center gap-3 mb-2">
          <motion.div
            animate={{ scale: [1, 1.2, 1] }}
            transition={{ duration: 1.5, repeat: Infinity }}
          >
            <Flame size={24} />
          </motion.div>
          <h3 className="text-lg font-semibold">Market Insights</h3>
        </div>
        <p className="text-white/80 text-sm">
          Skills in high demand and trending in the job market
        </p>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-100 dark:border-gray-700">
        <button
          onClick={() => setActiveTab('demand')}
          className={`flex-1 py-3 text-sm font-medium transition-colors flex items-center justify-center gap-2 ${
            activeTab === 'demand'
              ? 'text-purple-600 dark:text-purple-400 border-b-2 border-purple-600 dark:border-purple-400'
              : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
          }`}
        >
          <Briefcase size={16} />
          Top Demand
        </button>
        <button
          onClick={() => setActiveTab('rising')}
          className={`flex-1 py-3 text-sm font-medium transition-colors flex items-center justify-center gap-2 ${
            activeTab === 'rising'
              ? 'text-purple-600 dark:text-purple-400 border-b-2 border-purple-600 dark:border-purple-400'
              : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
          }`}
        >
          <Zap size={16} />
          Rising Fast
        </button>
      </div>

      {/* Content */}
      <div className="p-6">
        <div className="space-y-2">
          {activeTab === 'demand' ? (
            topDemandSkills && topDemandSkills.length > 0 ? (
              topDemandSkills.map((skill, idx) => (
                <SkillDemandCard 
                  key={skill.skillId} 
                  skill={skill} 
                  index={idx}
                  onExplain={onExplainSkill}
                />
              ))
            ) : (
              <p className="text-center text-gray-500 dark:text-gray-400 py-8">
                No market data available
              </p>
            )
          ) : (
            risingSkills && risingSkills.length > 0 ? (
              risingSkills.map((skill, idx) => (
                <SkillDemandCard 
                  key={skill.skillId} 
                  skill={skill} 
                  index={idx}
                  showGrowth
                  onExplain={onExplainSkill}
                />
              ))
            ) : (
              <p className="text-center text-gray-500 dark:text-gray-400 py-8">
                No rising skills data available
              </p>
            )
          )}
        </div>
      </div>
    </motion.div>
  );
}
