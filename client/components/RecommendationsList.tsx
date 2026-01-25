'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { LearningRecommendation, EnhancedRecommendations } from '@/lib/types';
import { useState } from 'react';
import { 
  BookOpen, 
  Clock, 
  ExternalLink, 
  GraduationCap,
  TrendingUp,
  Repeat,
  ChevronRight,
  Star,
  Zap,
  Target,
  Sparkles,
  ArrowUpRight,
  Lightbulb,
  Award
} from 'lucide-react';

interface RecommendationsListProps {
  recommendations: LearningRecommendation[];
  enhancedRecommendations?: EnhancedRecommendations;
}

const priorityColors: Record<number, { bg: string; text: string; border: string }> = {
  1: { bg: 'bg-red-100 dark:bg-red-900/30', text: 'text-red-700 dark:text-red-400', border: 'border-red-200 dark:border-red-800' },
  2: { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-700 dark:text-orange-400', border: 'border-orange-200 dark:border-orange-800' },
  3: { bg: 'bg-yellow-100 dark:bg-yellow-900/30', text: 'text-yellow-700 dark:text-yellow-400', border: 'border-yellow-200 dark:border-yellow-800' },
  4: { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-700 dark:text-blue-400', border: 'border-blue-200 dark:border-blue-800' },
  5: { bg: 'bg-gray-100 dark:bg-gray-800', text: 'text-gray-700 dark:text-gray-400', border: 'border-gray-200 dark:border-gray-700' },
};

const categoryConfig: Record<string, { icon: React.ReactNode; label: string; color: string; gradient: string }> = {
  learn_new: { 
    icon: <GraduationCap size={18} />, 
    label: 'New Skills to Learn', 
    color: 'text-purple-600 dark:text-purple-400',
    gradient: 'from-purple-500 to-indigo-600'
  },
  improve_existing: { 
    icon: <TrendingUp size={18} />, 
    label: 'Skills to Improve', 
    color: 'text-blue-600 dark:text-blue-400',
    gradient: 'from-blue-500 to-cyan-600'
  },
  practice_more: { 
    icon: <Repeat size={18} />, 
    label: 'Skills to Practice More', 
    color: 'text-green-600 dark:text-green-400',
    gradient: 'from-green-500 to-emerald-600'
  },
};

const impactColors: Record<string, { bg: string; text: string }> = {
  high: { bg: 'bg-purple-100 dark:bg-purple-900/30', text: 'text-purple-700 dark:text-purple-400' },
  medium: { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-700 dark:text-blue-400' },
  low: { bg: 'bg-gray-100 dark:bg-gray-800', text: 'text-gray-700 dark:text-gray-400' },
};

function RecommendationCard({ rec, index }: { rec: LearningRecommendation; index: number }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const colors = priorityColors[rec.priority] || priorityColors[5];
  const category = categoryConfig[rec.category || 'learn_new'];
  const impact = impactColors[rec.careerImpact || 'medium'];

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.4, delay: index * 0.1 }}
      whileHover={{ scale: 1.01, x: 4 }}
      className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden hover:shadow-lg transition-all duration-300 card-hover"
    >
      {/* Colored top bar */}
      <div className={`h-1 bg-gradient-to-r ${category.gradient}`} />
      
      <div 
        className="p-5 cursor-pointer"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-2">
              <motion.div
                whileHover={{ rotate: 360 }}
                transition={{ duration: 0.5 }}
                className={`p-2 rounded-lg bg-gradient-to-br ${category.gradient} text-white`}
              >
                {category.icon}
              </motion.div>
              <div>
                <h4 className="font-semibold text-gray-900 dark:text-white text-lg">
                  {rec.skill}
                </h4>
                <span className={`text-xs ${category.color}`}>
                  {category.label}
                </span>
              </div>
            </div>
            
            <p className="text-gray-600 dark:text-gray-400 mb-3 text-sm leading-relaxed">
              {rec.reason}
            </p>

            <div className="flex items-center flex-wrap gap-3 text-sm">
              <span className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full ${colors.bg} ${colors.text} font-medium`}>
                <Star size={12} />
                Priority {rec.priority}
              </span>
              <span className="flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
                <Clock size={14} />
                {rec.estimatedTimeToLearn}
              </span>
              {rec.difficultyLevel && (
                <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 capitalize">
                  <Target size={12} />
                  {rec.difficultyLevel}
                </span>
              )}
              {rec.careerImpact && (
                <span className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full ${impact.bg} ${impact.text}`}>
                  <Zap size={12} />
                  {rec.careerImpact} impact
                </span>
              )}
            </div>
          </div>
          
          <motion.div
            animate={{ rotate: isExpanded ? 90 : 0 }}
            className="text-gray-400 mt-2"
          >
            <ChevronRight size={20} />
          </motion.div>
        </div>

        <AnimatePresence>
          {isExpanded && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.3 }}
              className="overflow-hidden"
            >
              {/* Related Skills */}
              {rec.relatedSkills && rec.relatedSkills.length > 0 && (
                <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-700">
                  <p className="text-xs text-gray-500 dark:text-gray-400 mb-2 flex items-center gap-1">
                    <Sparkles size={12} />
                    Related to your existing skills:
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {rec.relatedSkills.map((skill, idx) => (
                      <span
                        key={idx}
                        className="px-2 py-1 bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400 rounded text-xs"
                      >
                        {skill}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              
              {/* Resources */}
              {rec.resources && rec.resources.length > 0 && (
                <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-700">
                  <p className="text-xs text-gray-500 dark:text-gray-400 mb-2 flex items-center gap-1">
                    <BookOpen size={12} />
                    Suggested Resources:
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {rec.resources.map((resource, ridx) => (
                      <motion.span
                        key={ridx}
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: ridx * 0.05 }}
                        className="inline-flex items-center gap-1 px-3 py-1.5 bg-gradient-to-r from-purple-50 to-indigo-50 dark:from-purple-900/20 dark:to-indigo-900/20 text-purple-700 dark:text-purple-400 rounded-lg text-xs hover:from-purple-100 hover:to-indigo-100 dark:hover:from-purple-900/30 dark:hover:to-indigo-900/30 transition-colors cursor-pointer"
                      >
                        <ExternalLink size={10} />
                        {resource}
                        <ArrowUpRight size={10} />
                      </motion.span>
                    ))}
                  </div>
                </div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}

function CategorySection({ 
  title, 
  recommendations, 
  icon, 
  gradient,
  description 
}: { 
  title: string; 
  recommendations: LearningRecommendation[];
  icon: React.ReactNode;
  gradient: string;
  description: string;
}) {
  if (!recommendations || recommendations.length === 0) return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="mb-8"
    >
      <div className="flex items-center gap-3 mb-4">
        <motion.div 
          className={`p-2.5 rounded-xl bg-gradient-to-br ${gradient} text-white`}
          whileHover={{ scale: 1.1, rotate: 5 }}
        >
          {icon}
        </motion.div>
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            {title}
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {description}
          </p>
        </div>
        <span className="ml-auto px-3 py-1 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 rounded-full text-sm">
          {recommendations.length}
        </span>
      </div>
      
      <div className="space-y-4">
        {recommendations.map((rec, idx) => (
          <RecommendationCard key={rec.skill} rec={rec} index={idx} />
        ))}
      </div>
    </motion.div>
  );
}

export function RecommendationsList({ recommendations, enhancedRecommendations }: RecommendationsListProps) {
  // Use enhanced recommendations if available, otherwise fall back to regular recommendations
  const hasEnhanced = enhancedRecommendations && (
    (enhancedRecommendations.skillsToLearn?.length || 0) > 0 ||
    (enhancedRecommendations.skillsToImprove?.length || 0) > 0 ||
    (enhancedRecommendations.practiceMore?.length || 0) > 0
  );

  const totalRecs = hasEnhanced 
    ? (enhancedRecommendations?.skillsToLearn?.length || 0) + 
      (enhancedRecommendations?.skillsToImprove?.length || 0) + 
      (enhancedRecommendations?.practiceMore?.length || 0)
    : recommendations?.length || 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
      >
        <div className="flex items-center gap-3">
          <motion.div 
            className="p-3 rounded-xl bg-gradient-to-br from-purple-500 to-indigo-600 text-white"
            animate={{ scale: [1, 1.05, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
          >
            <Lightbulb size={24} />
          </motion.div>
          <div>
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">
              Learning Roadmap
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {totalRecs} personalized recommendations
            </p>
          </div>
        </div>
      </motion.div>

      {hasEnhanced ? (
        <>
          {/* Skills to Learn */}
          <CategorySection
            title="New Skills to Learn"
            recommendations={enhancedRecommendations?.skillsToLearn || []}
            icon={<GraduationCap size={20} />}
            gradient="from-purple-500 to-indigo-600"
            description="Start your journey with these new skills"
          />

          {/* Skills to Improve */}
          <CategorySection
            title="Skills to Deepen"
            recommendations={enhancedRecommendations?.skillsToImprove || []}
            icon={<TrendingUp size={20} />}
            gradient="from-blue-500 to-cyan-600"
            description="Take your existing skills to the next level"
          />

          {/* Skills to Practice More */}
          <CategorySection
            title="Practice Makes Perfect"
            recommendations={enhancedRecommendations?.practiceMore || []}
            icon={<Repeat size={20} />}
            gradient="from-green-500 to-emerald-600"
            description="Build real-world experience with these skills"
          />

          {/* Career Advice */}
          {enhancedRecommendations?.careerAdvice && enhancedRecommendations.careerAdvice.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="p-5 bg-gradient-to-br from-amber-50 to-orange-50 dark:from-amber-900/20 dark:to-orange-900/20 rounded-xl border border-amber-200 dark:border-amber-800"
            >
              <h4 className="font-semibold text-amber-900 dark:text-amber-300 mb-3 flex items-center gap-2">
                <Award size={18} />
                Career Advice
              </h4>
              <ul className="space-y-2">
                {enhancedRecommendations.careerAdvice.map((advice, idx) => (
                  <motion.li
                    key={idx}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.4 + idx * 0.1 }}
                    className="flex items-start gap-2 text-amber-800 dark:text-amber-400 text-sm"
                  >
                    <ChevronRight size={14} className="mt-1 flex-shrink-0" />
                    {advice}
                  </motion.li>
                ))}
              </ul>
            </motion.div>
          )}

          {/* Next Milestone */}
          {enhancedRecommendations?.nextMilestone && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.5 }}
              className="p-5 bg-gradient-to-r from-purple-600 to-indigo-600 rounded-xl text-white"
            >
              <h4 className="font-semibold mb-2 flex items-center gap-2">
                <Target size={18} />
                Next Career Milestone
              </h4>
              <p className="text-purple-100">
                {enhancedRecommendations.nextMilestone}
              </p>
            </motion.div>
          )}
        </>
      ) : (
        // Fallback to regular recommendations
        <div className="space-y-4">
          {recommendations?.sort((a, b) => a.priority - b.priority).map((rec, idx) => (
            <RecommendationCard key={rec.skill} rec={rec} index={idx} />
          ))}
        </div>
      )}
    </div>
  );
}
