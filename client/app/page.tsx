'use client';

import { useAuth } from '@/lib/auth-context';
import { motion } from 'framer-motion';
import Link from 'next/link';
import { ArrowRight, Github, Brain, Target, Zap, CheckCircle, Sparkles, Code, Layers } from 'lucide-react';

export default function Home() {
  const { isAuthenticated } = useAuth();

  const features = [
    {
      icon: Github,
      title: 'GitHub Integration',
      description: 'Connect your GitHub account to automatically fetch and analyze your repositories.',
      color: 'from-gray-700 to-gray-900',
    },
    {
      icon: Brain,
      title: 'AI-Powered Analysis',
      description: 'Our AI analyzes your READMEs and project metadata to identify your skill set.',
      color: 'from-purple-500 to-indigo-600',
    },
    {
      icon: Target,
      title: 'Skill Gap Detection',
      description: 'Discover what skills you should learn next to advance your career.',
      color: 'from-orange-500 to-red-500',
    },
    {
      icon: Zap,
      title: 'Personalized Roadmap',
      description: 'Get actionable learning recommendations tailored to your experience.',
      color: 'from-green-500 to-emerald-600',
    },
  ];

  const benefits = [
    'No code access required - we only analyze READMEs',
    'Token-efficient AI processing',
    'Persistent analysis history',
    'Privacy-focused approach',
  ];

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: { staggerChildren: 0.1, delayChildren: 0.2 },
    },
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.5 } },
  };

  return (
    <div className="min-h-screen overflow-hidden">
      {/* Hero Section */}
      <section className="relative overflow-hidden">
        {/* Animated background */}
        <div className="absolute inset-0 bg-gradient-to-br from-purple-600/20 to-indigo-600/20 dark:from-purple-900/40 dark:to-indigo-900/40" />
        <motion.div
          className="absolute top-20 left-10 w-72 h-72 bg-purple-500/30 rounded-full blur-3xl"
          animate={{ scale: [1, 1.2, 1], x: [0, 50, 0], y: [0, 30, 0] }}
          transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}
        />
        <motion.div
          className="absolute bottom-20 right-10 w-96 h-96 bg-indigo-500/20 rounded-full blur-3xl"
          animate={{ scale: [1.2, 1, 1.2], x: [0, -30, 0], y: [0, -50, 0] }}
          transition={{ duration: 10, repeat: Infinity, ease: 'easeInOut' }}
        />
        
        {/* Floating icons */}
        <motion.div
          className="absolute top-32 left-[15%] text-purple-500/20"
          animate={{ y: [0, -20, 0], rotate: [0, 10, 0] }}
          transition={{ duration: 5, repeat: Infinity }}
        >
          <Code size={48} />
        </motion.div>
        <motion.div
          className="absolute top-48 right-[20%] text-indigo-500/20"
          animate={{ y: [0, 20, 0], rotate: [0, -10, 0] }}
          transition={{ duration: 6, repeat: Infinity }}
        >
          <Layers size={40} />
        </motion.div>
        <motion.div
          className="absolute bottom-32 left-[25%] text-purple-500/20"
          animate={{ y: [0, 15, 0], rotate: [0, 15, 0] }}
          transition={{ duration: 4, repeat: Infinity }}
        >
          <Sparkles size={36} />
        </motion.div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 lg:py-32">
          <motion.div 
            className="text-center"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
          >
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', delay: 0.2 }}
              className="inline-flex items-center gap-2 px-4 py-2 bg-purple-100 dark:bg-purple-900/30 rounded-full text-purple-700 dark:text-purple-300 text-sm font-medium mb-6"
            >
              <Sparkles size={16} />
              AI-Powered Skill Analysis
            </motion.div>
            
            <motion.h1 
              className="text-4xl md:text-6xl lg:text-7xl font-bold text-gray-900 dark:text-white mb-6"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
            >
              Level Up Your
              <motion.span 
                className="text-transparent bg-clip-text bg-gradient-to-r from-purple-600 to-indigo-600 block sm:inline"
                animate={{ backgroundPosition: ['0%', '100%', '0%'] }}
                transition={{ duration: 5, repeat: Infinity }}
              >
                {' '}Developer Skills
              </motion.span>
            </motion.h1>
            
            <motion.p 
              className="text-xl text-gray-600 dark:text-gray-300 max-w-3xl mx-auto mb-10"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.4 }}
            >
              GitUpskill analyzes your GitHub projects to identify your strengths, 
              detect skill gaps, and recommend personalized learning paths.
            </motion.p>
            
            <motion.div 
              className="flex flex-col sm:flex-row gap-4 justify-center"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
            >
              <Link href={isAuthenticated ? '/dashboard' : '/login'}>
                <motion.button
                  whileHover={{ scale: 1.05, boxShadow: '0 20px 40px rgba(147, 51, 234, 0.3)' }}
                  whileTap={{ scale: 0.95 }}
                  className="inline-flex items-center justify-center gap-2 bg-gradient-to-r from-purple-600 to-indigo-600 text-white px-8 py-4 rounded-xl text-lg font-medium shadow-lg shadow-purple-500/25"
                >
                  {isAuthenticated ? 'Go to Dashboard' : 'Get Started Free'}
                  <motion.div animate={{ x: [0, 5, 0] }} transition={{ duration: 1.5, repeat: Infinity }}>
                    <ArrowRight size={20} />
                  </motion.div>
                </motion.button>
              </Link>
              <motion.a
                href="#features"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="inline-flex items-center justify-center gap-2 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-8 py-4 rounded-xl text-lg font-medium border border-gray-200 dark:border-gray-700 shadow-lg"
              >
                Learn More
              </motion.a>
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 bg-white dark:bg-gray-900">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <motion.div 
            className="text-center mb-16"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <motion.span 
              className="text-purple-600 font-medium"
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
            >
              How It Works
            </motion.span>
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 dark:text-white mb-4 mt-2">
              Simple Yet Powerful Workflow
            </h2>
            <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
              A simple yet powerful workflow to understand and improve your developer skills.
            </p>
          </motion.div>

          <motion.div 
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6"
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
          >
            {features.map((feature, idx) => (
              <motion.div
                key={idx}
                variants={itemVariants}
                whileHover={{ y: -8, scale: 1.02 }}
                className="relative bg-gray-50 dark:bg-gray-800 rounded-2xl p-6 hover:shadow-xl transition-shadow group overflow-hidden"
              >
                <motion.div
                  className="absolute inset-0 bg-gradient-to-br opacity-0 group-hover:opacity-5 transition-opacity"
                  style={{ backgroundImage: `linear-gradient(to bottom right, var(--tw-gradient-stops))` }}
                />
                <motion.div 
                  className={`w-14 h-14 bg-gradient-to-br ${feature.color} rounded-xl flex items-center justify-center mb-4 shadow-lg`}
                  whileHover={{ rotate: 360 }}
                  transition={{ duration: 0.5 }}
                >
                  <feature.icon className="text-white" size={24} />
                </motion.div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                  {feature.title}
                </h3>
                <p className="text-gray-600 dark:text-gray-400">
                  {feature.description}
                </p>
                <motion.div 
                  className="absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-purple-500 to-indigo-500 origin-left"
                  initial={{ scaleX: 0 }}
                  whileHover={{ scaleX: 1 }}
                  transition={{ duration: 0.3 }}
                />
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="py-20 bg-gray-50 dark:bg-gray-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6 }}
            >
              <span className="text-purple-600 font-medium">Why Choose Us</span>
              <h2 className="text-3xl md:text-4xl font-bold text-gray-900 dark:text-white mb-6 mt-2">
                Privacy-First Approach
              </h2>
              <p className="text-lg text-gray-600 dark:text-gray-400 mb-8">
                We only analyze your README files and repository metadata. 
                Your source code is never accessed, ensuring complete privacy 
                while still providing accurate skill insights.
              </p>
              <motion.ul 
                className="space-y-4"
                variants={containerVariants}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
              >
                {benefits.map((benefit, idx) => (
                  <motion.li 
                    key={idx} 
                    className="flex items-center gap-3"
                    variants={itemVariants}
                  >
                    <motion.div
                      whileHover={{ scale: 1.2, rotate: 360 }}
                      transition={{ duration: 0.3 }}
                    >
                      <CheckCircle className="text-green-500 flex-shrink-0" size={20} />
                    </motion.div>
                    <span className="text-gray-700 dark:text-gray-300">{benefit}</span>
                  </motion.li>
                ))}
              </motion.ul>
            </motion.div>
            
            <motion.div 
              className="relative"
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6 }}
            >
              <motion.div
                className="absolute -inset-4 bg-gradient-to-r from-purple-500 to-indigo-500 rounded-3xl blur-2xl opacity-20"
                animate={{ scale: [1, 1.05, 1] }}
                transition={{ duration: 4, repeat: Infinity }}
              />
              <div className="relative bg-gradient-to-br from-purple-600 to-indigo-700 rounded-2xl p-8 text-white shadow-2xl">
                <h3 className="text-2xl font-bold mb-6">What You&apos;ll Get</h3>
                <ul className="space-y-5">
                  {[
                    { emoji: '📊', title: 'Skill Assessment', desc: 'Detailed breakdown of your technical skills' },
                    { emoji: '🎯', title: 'Gap Analysis', desc: 'Identify missing skills in your profile' },
                    { emoji: '🗺️', title: 'Learning Roadmap', desc: 'Prioritized recommendations for growth' },
                    { emoji: '👤', title: 'Developer Profile', desc: 'Experience level and specialization insights' },
                  ].map((item, idx) => (
                    <motion.li 
                      key={idx}
                      className="flex items-start gap-4"
                      initial={{ opacity: 0, x: 20 }}
                      whileInView={{ opacity: 1, x: 0 }}
                      viewport={{ once: true }}
                      transition={{ delay: idx * 0.1 }}
                    >
                      <motion.span 
                        className="text-2xl"
                        whileHover={{ scale: 1.3, rotate: 10 }}
                      >
                        {item.emoji}
                      </motion.span>
                      <div>
                        <strong>{item.title}</strong>
                        <p className="text-purple-200 text-sm">{item.desc}</p>
                      </div>
                    </motion.li>
                  ))}
                </ul>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600 to-indigo-700" />
        <motion.div
          className="absolute top-0 left-0 w-full h-full"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          <motion.div
            className="absolute top-10 left-10 w-32 h-32 bg-white/10 rounded-full blur-2xl"
            animate={{ scale: [1, 1.5, 1], x: [0, 100, 0] }}
            transition={{ duration: 10, repeat: Infinity }}
          />
          <motion.div
            className="absolute bottom-10 right-10 w-48 h-48 bg-white/10 rounded-full blur-3xl"
            animate={{ scale: [1.5, 1, 1.5], x: [0, -50, 0] }}
            transition={{ duration: 8, repeat: Infinity }}
          />
        </motion.div>
        
        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <motion.div
              animate={{ rotate: [0, 5, -5, 0] }}
              transition={{ duration: 4, repeat: Infinity }}
              className="inline-block mb-4"
            >
              <Sparkles size={40} className="text-white/80" />
            </motion.div>
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
              Ready to Discover Your Skills?
            </h2>
            <p className="text-xl text-purple-200 mb-8">
              Connect your GitHub and get your personalized skill analysis in minutes.
            </p>
            <Link href={isAuthenticated ? '/dashboard' : '/login'}>
              <motion.button
                whileHover={{ scale: 1.05, boxShadow: '0 20px 40px rgba(0, 0, 0, 0.3)' }}
                whileTap={{ scale: 0.95 }}
                className="inline-flex items-center justify-center gap-3 bg-white text-purple-700 px-8 py-4 rounded-xl text-lg font-medium shadow-xl"
              >
                <Github size={24} />
                {isAuthenticated ? 'Go to Dashboard' : 'Connect with GitHub'}
              </motion.button>
            </Link>
          </motion.div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-400 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.div 
            className="flex items-center justify-center gap-2 mb-4"
            whileHover={{ scale: 1.05 }}
          >
            <motion.div
              animate={{ rotate: [0, 360] }}
              transition={{ duration: 20, repeat: Infinity, ease: 'linear' }}
            >
              <Zap className="text-purple-500" size={24} />
            </motion.div>
            <span className="text-xl font-bold text-white">GitUpskill</span>
          </motion.div>
          <p className="text-sm">
            AI-Powered Developer Skill & Learning Recommendation Platform
          </p>
          <p className="text-xs mt-4">
            © {new Date().getFullYear()} GitUpskill. Built with ❤️ for developers.
          </p>
        </div>
      </footer>
    </div>
  );
}
