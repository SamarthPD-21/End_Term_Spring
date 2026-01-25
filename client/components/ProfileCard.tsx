'use client';

import { motion } from 'framer-motion';
import { DeveloperProfile } from '@/lib/types';
import { Code, Layers, Target, TrendingUp, Award, Sparkles } from 'lucide-react';

interface ProfileCardProps {
  profile: DeveloperProfile;
  repositoriesAnalyzed: number;
}

export function ProfileCard({ profile, repositoriesAnalyzed }: ProfileCardProps) {
  const levelColors: Record<string, { bg: string; glow: string }> = {
    Junior: { 
      bg: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
      glow: 'shadow-green-500/30'
    },
    Mid: { 
      bg: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
      glow: 'shadow-blue-500/30'
    },
    Senior: { 
      bg: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
      glow: 'shadow-purple-500/30'
    },
  };

  const levelConfig = levelColors[profile.experienceLevel] || { bg: 'bg-gray-100 text-gray-700', glow: '' };

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1,
        delayChildren: 0.1,
      },
    },
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  };

  return (
    <motion.div 
      className="relative overflow-hidden bg-gradient-to-br from-purple-600 via-purple-700 to-indigo-700 rounded-2xl p-6 text-white"
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.4 }}
    >
      {/* Animated background elements */}
      <div className="absolute inset-0 overflow-hidden">
        <motion.div
          className="absolute -top-20 -right-20 w-40 h-40 bg-white/10 rounded-full blur-3xl"
          animate={{ 
            scale: [1, 1.2, 1],
            opacity: [0.3, 0.5, 0.3],
          }}
          transition={{ duration: 4, repeat: Infinity }}
        />
        <motion.div
          className="absolute -bottom-20 -left-20 w-60 h-60 bg-purple-500/20 rounded-full blur-3xl"
          animate={{ 
            scale: [1.2, 1, 1.2],
            opacity: [0.2, 0.4, 0.2],
          }}
          transition={{ duration: 5, repeat: Infinity }}
        />
      </div>

      <div className="relative z-10">
        <motion.div 
          className="flex items-start justify-between mb-6"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          <motion.div variants={itemVariants}>
            <div className="flex items-center gap-2 mb-1">
              <motion.div
                animate={{ rotate: [0, 10, -10, 0] }}
                transition={{ duration: 2, repeat: Infinity }}
              >
                <Award size={24} />
              </motion.div>
              <h2 className="text-2xl font-bold">Developer Profile</h2>
            </div>
            <p className="text-purple-200">Based on {repositoriesAnalyzed} repositories</p>
          </motion.div>
          
          <motion.span 
            variants={itemVariants}
            whileHover={{ scale: 1.05 }}
            className={`px-4 py-1.5 rounded-full text-sm font-semibold ${levelConfig.bg} shadow-lg ${levelConfig.glow}`}
          >
            <span className="flex items-center gap-1">
              <Sparkles size={14} />
              {profile.experienceLevel} Developer
            </span>
          </motion.span>
        </motion.div>

        <motion.div 
          className="grid grid-cols-1 md:grid-cols-2 gap-4"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {/* Primary Languages */}
          <motion.div 
            variants={itemVariants}
            whileHover={{ scale: 1.02, backgroundColor: 'rgba(255,255,255,0.15)' }}
            className="bg-white/10 backdrop-blur-sm rounded-xl p-4 transition-colors"
          >
            <div className="flex items-center gap-2 mb-3">
              <motion.div
                whileHover={{ rotate: 360 }}
                transition={{ duration: 0.5 }}
                className="p-1.5 bg-white/20 rounded-lg"
              >
                <Code size={16} />
              </motion.div>
              <h3 className="font-semibold">Primary Languages</h3>
            </div>
            <div className="flex flex-wrap gap-2">
              {profile.primaryLanguages.map((lang, idx) => (
                <motion.span 
                  key={idx} 
                  className="px-3 py-1 bg-white/20 hover:bg-white/30 rounded-full text-sm cursor-default transition-colors"
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: idx * 0.05 }}
                  whileHover={{ scale: 1.1 }}
                >
                  {lang}
                </motion.span>
              ))}
            </div>
          </motion.div>

          {/* Primary Frameworks */}
          <motion.div 
            variants={itemVariants}
            whileHover={{ scale: 1.02, backgroundColor: 'rgba(255,255,255,0.15)' }}
            className="bg-white/10 backdrop-blur-sm rounded-xl p-4 transition-colors"
          >
            <div className="flex items-center gap-2 mb-3">
              <motion.div
                whileHover={{ rotate: 360 }}
                transition={{ duration: 0.5 }}
                className="p-1.5 bg-white/20 rounded-lg"
              >
                <Layers size={16} />
              </motion.div>
              <h3 className="font-semibold">Primary Frameworks</h3>
            </div>
            <div className="flex flex-wrap gap-2">
              {profile.primaryFrameworks.map((fw, idx) => (
                <motion.span 
                  key={idx} 
                  className="px-3 py-1 bg-white/20 hover:bg-white/30 rounded-full text-sm cursor-default transition-colors"
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: idx * 0.05 }}
                  whileHover={{ scale: 1.1 }}
                >
                  {fw}
                </motion.span>
              ))}
            </div>
          </motion.div>

          {/* Project Types */}
          <motion.div 
            variants={itemVariants}
            whileHover={{ scale: 1.02, backgroundColor: 'rgba(255,255,255,0.15)' }}
            className="bg-white/10 backdrop-blur-sm rounded-xl p-4 transition-colors"
          >
            <div className="flex items-center gap-2 mb-3">
              <motion.div
                whileHover={{ rotate: 360 }}
                transition={{ duration: 0.5 }}
                className="p-1.5 bg-white/20 rounded-lg"
              >
                <Target size={16} />
              </motion.div>
              <h3 className="font-semibold">Project Types</h3>
            </div>
            <div className="flex flex-wrap gap-2">
              {profile.projectTypes.map((type, idx) => (
                <motion.span 
                  key={idx} 
                  className="px-3 py-1 bg-white/20 hover:bg-white/30 rounded-full text-sm cursor-default transition-colors"
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: idx * 0.05 }}
                  whileHover={{ scale: 1.1 }}
                >
                  {type}
                </motion.span>
              ))}
            </div>
          </motion.div>

          {/* Specialization */}
          <motion.div 
            variants={itemVariants}
            whileHover={{ scale: 1.02, backgroundColor: 'rgba(255,255,255,0.15)' }}
            className="bg-white/10 backdrop-blur-sm rounded-xl p-4 transition-colors"
          >
            <div className="flex items-center gap-2 mb-3">
              <motion.div
                whileHover={{ rotate: 360 }}
                transition={{ duration: 0.5 }}
                className="p-1.5 bg-white/20 rounded-lg"
              >
                <TrendingUp size={16} />
              </motion.div>
              <h3 className="font-semibold">Specialization</h3>
            </div>
            <motion.p 
              className="text-lg capitalize font-medium"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3 }}
            >
              {profile.specialization}
            </motion.p>
          </motion.div>
        </motion.div>

        {/* Skill Distribution */}
        {profile.skillDistribution && Object.keys(profile.skillDistribution).length > 0 && (
          <motion.div 
            className="mt-6 bg-white/10 backdrop-blur-sm rounded-xl p-4"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
          >
            <h3 className="font-semibold mb-4 flex items-center gap-2">
              <Sparkles size={16} />
              Skill Distribution
            </h3>
            <div className="space-y-3">
              {Object.entries(profile.skillDistribution).map(([category, value], idx) => (
                <motion.div 
                  key={category}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.6 + idx * 0.1 }}
                >
                  <div className="flex justify-between text-sm mb-1">
                    <span className="capitalize">{category}</span>
                    <motion.span
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      transition={{ delay: 0.8 + idx * 0.1 }}
                    >
                      {value}%
                    </motion.span>
                  </div>
                  <div className="w-full bg-white/20 rounded-full h-2 overflow-hidden">
                    <motion.div
                      className="bg-gradient-to-r from-white to-purple-200 rounded-full h-2"
                      initial={{ width: 0 }}
                      animate={{ width: `${value}%` }}
                      transition={{ duration: 0.8, delay: 0.7 + idx * 0.1, ease: 'easeOut' }}
                    />
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}
