'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { Skill, SkillAnalysis } from '@/lib/types';
import { useState } from 'react';
import { 
  TrendingUp, 
  TrendingDown, 
  Minus, 
  Code, 
  Layers, 
  Wrench, 
  Lightbulb,
  ChevronDown,
  ChevronUp,
  Sparkles
} from 'lucide-react';

interface SkillCardProps {
  skill: Skill;
  type: 'strong' | 'moderate' | 'weak';
  index: number;
}

const categoryIcons: Record<string, React.ReactNode> = {
  language: <Code size={14} />,
  framework: <Layers size={14} />,
  tool: <Wrench size={14} />,
  concept: <Lightbulb size={14} />,
  methodology: <Sparkles size={14} />,
};

const trendIcons: Record<string, React.ReactNode> = {
  improving: <TrendingUp size={12} className="text-green-500" />,
  declining: <TrendingDown size={12} className="text-red-500" />,
  stable: <Minus size={12} className="text-gray-400" />,
};

function SkillCard({ skill, type, index }: SkillCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  
  const colors = {
    strong: {
      bg: 'bg-gradient-to-br from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20',
      border: 'border-green-200 dark:border-green-800',
      progress: 'bg-gradient-to-r from-green-400 to-emerald-500',
      glow: 'hover:shadow-green-200/50 dark:hover:shadow-green-900/30',
    },
    moderate: {
      bg: 'bg-gradient-to-br from-amber-50 to-yellow-50 dark:from-amber-900/20 dark:to-yellow-900/20',
      border: 'border-amber-200 dark:border-amber-800',
      progress: 'bg-gradient-to-r from-amber-400 to-yellow-500',
      glow: 'hover:shadow-amber-200/50 dark:hover:shadow-amber-900/30',
    },
    weak: {
      bg: 'bg-gradient-to-br from-red-50 to-rose-50 dark:from-red-900/20 dark:to-rose-900/20',
      border: 'border-red-200 dark:border-red-800',
      progress: 'bg-gradient-to-r from-red-400 to-rose-500',
      glow: 'hover:shadow-red-200/50 dark:hover:shadow-red-900/30',
    },
  };

  const cardStyle = colors[type];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ 
        duration: 0.4, 
        delay: index * 0.05,
        type: "spring",
        stiffness: 100
      }}
      whileHover={{ scale: 1.02, y: -2 }}
      className={`p-4 rounded-xl border ${cardStyle.bg} ${cardStyle.border} ${cardStyle.glow} 
        transition-all duration-300 cursor-pointer hover:shadow-lg skill-card-glow`}
      onClick={() => setIsExpanded(!isExpanded)}
    >
      <div className="flex justify-between items-start mb-3">
        <div className="flex items-center gap-2">
          <motion.h4 
            className="font-semibold text-gray-900 dark:text-white"
            layoutId={`skill-name-${skill.name}`}
          >
            {skill.name}
          </motion.h4>
          {skill.trend && trendIcons[skill.trend]}
        </div>
        <div className="flex items-center gap-2">
          <span className="flex items-center gap-1 text-xs px-2 py-1 rounded-full bg-white/60 dark:bg-gray-800/60 text-gray-600 dark:text-gray-400">
            {categoryIcons[skill.category] || categoryIcons.concept}
            <span className="capitalize">{skill.category}</span>
          </span>
        </div>
      </div>
      
      <div className="mb-3">
        <div className="flex justify-between text-xs text-gray-600 dark:text-gray-400 mb-1.5">
          <span className="flex items-center gap-1">
            Proficiency
            {skill.projectCount && (
              <span className="text-purple-600 dark:text-purple-400">
                ({skill.projectCount} project{skill.projectCount > 1 ? 's' : ''})
              </span>
            )}
          </span>
          <span className="font-medium">{skill.proficiencyScore}%</span>
        </div>
        <div className="w-full bg-gray-200/60 dark:bg-gray-700/60 rounded-full h-2.5 overflow-hidden">
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: `${skill.proficiencyScore}%` }}
            transition={{ duration: 0.8, delay: index * 0.05 + 0.2, ease: "easeOut" }}
            className={`h-2.5 rounded-full ${cardStyle.progress}`}
          />
        </div>
      </div>
      
      <AnimatePresence>
        {skill.evidence && (
          <motion.div
            initial={isExpanded ? { height: "auto", opacity: 1 } : { height: 0, opacity: 0 }}
            animate={isExpanded ? { height: "auto", opacity: 1 } : { height: "1.5rem", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden"
          >
            <p className={`text-xs text-gray-600 dark:text-gray-400 ${!isExpanded && 'line-clamp-1'}`}>
              {skill.evidence}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
      
      {skill.evidence && (
        <motion.div 
          className="flex justify-center mt-2"
          animate={{ rotate: isExpanded ? 180 : 0 }}
        >
          <ChevronDown size={14} className="text-gray-400" />
        </motion.div>
      )}
    </motion.div>
  );
}

interface SkillSectionProps {
  title: string;
  skills: Skill[];
  type: 'strong' | 'moderate' | 'weak';
  color: string;
  icon: React.ReactNode;
  defaultExpanded?: boolean;
}

function SkillSection({ title, skills, type, color, icon, defaultExpanded = true }: SkillSectionProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  
  if (skills.length === 0) return null;
  
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="mb-8"
    >
      <motion.button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full flex items-center justify-between mb-4 group"
        whileHover={{ x: 4 }}
      >
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-3">
          <motion.span 
            className={`w-4 h-4 rounded-full ${color} flex items-center justify-center`}
            animate={{ scale: [1, 1.2, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
          >
            {icon}
          </motion.span>
          {title}
          <span className="text-sm font-normal text-gray-500 dark:text-gray-400">
            ({skills.length})
          </span>
        </h3>
        <motion.div
          animate={{ rotate: isExpanded ? 180 : 0 }}
          transition={{ duration: 0.3 }}
        >
          <ChevronUp className="text-gray-400 group-hover:text-gray-600 dark:group-hover:text-gray-300" size={20} />
        </motion.div>
      </motion.button>
      
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 overflow-hidden"
          >
            {skills.map((skill, idx) => (
              <SkillCard key={`${skill.name}-${idx}`} skill={skill} type={type} index={idx} />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

interface SkillsDisplayProps {
  skillAnalysis: SkillAnalysis;
}

export function SkillsDisplay({ skillAnalysis }: SkillsDisplayProps) {
  const totalSkills = (skillAnalysis.strongSkills?.length || 0) + 
                      (skillAnalysis.moderateSkills?.length || 0) + 
                      (skillAnalysis.weakSkills?.length || 0);

  return (
    <div className="space-y-6">
      {/* Skills Overview Header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6"
      >
        <div>
          <h2 className="text-xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
            <Sparkles className="text-purple-600" size={24} />
            Skills Analysis
          </h2>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            {totalSkills} skills detected across your projects
          </p>
        </div>
        
        {/* Skills Summary Pills */}
        <div className="flex gap-2 flex-wrap">
          <motion.span 
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.2 }}
            className="px-3 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 rounded-full text-sm font-medium"
          >
            {skillAnalysis.strongSkills?.length || 0} Strong
          </motion.span>
          <motion.span 
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.3 }}
            className="px-3 py-1 bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 rounded-full text-sm font-medium"
          >
            {skillAnalysis.moderateSkills?.length || 0} Moderate
          </motion.span>
          <motion.span 
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.4 }}
            className="px-3 py-1 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 rounded-full text-sm font-medium"
          >
            {skillAnalysis.weakSkills?.length || 0} Learning
          </motion.span>
        </div>
      </motion.div>

      {/* Strong Skills */}
      <SkillSection
        title="Strong Skills"
        skills={skillAnalysis.strongSkills || []}
        type="strong"
        color="bg-green-500"
        icon={<TrendingUp size={10} className="text-white" />}
        defaultExpanded={true}
      />

      {/* Moderate Skills */}
      <SkillSection
        title="Growing Skills"
        skills={skillAnalysis.moderateSkills || []}
        type="moderate"
        color="bg-amber-500"
        icon={<Minus size={10} className="text-white" />}
        defaultExpanded={true}
      />

      {/* Weak Skills */}
      <SkillSection
        title="Beginner Skills"
        skills={skillAnalysis.weakSkills || []}
        type="weak"
        color="bg-red-500"
        icon={<TrendingDown size={10} className="text-white" />}
        defaultExpanded={false}
      />

      {/* Missing Skills */}
      {skillAnalysis.missingSkills && skillAnalysis.missingSkills.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="mt-6 p-4 bg-gradient-to-r from-purple-50 to-indigo-50 dark:from-purple-900/20 dark:to-indigo-900/20 rounded-xl border border-purple-200 dark:border-purple-800"
        >
          <h4 className="text-sm font-semibold text-purple-900 dark:text-purple-300 mb-3 flex items-center gap-2">
            <Lightbulb size={16} />
            Skills to Explore
          </h4>
          <div className="flex flex-wrap gap-2">
            {skillAnalysis.missingSkills.map((skill, idx) => (
              <motion.span
                key={skill}
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.6 + idx * 0.1 }}
                className="px-3 py-1.5 bg-white dark:bg-gray-800 text-purple-700 dark:text-purple-400 rounded-lg text-sm border border-purple-200 dark:border-purple-700 hover:border-purple-400 dark:hover:border-purple-500 transition-colors cursor-pointer"
              >
                {skill}
              </motion.span>
            ))}
          </div>
        </motion.div>
      )}
    </div>
  );
}
