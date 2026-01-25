'use client';

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { AnalysisRequest, Repository } from '@/lib/types';
import { api } from '@/lib/api';
import { Play, Loader2, Calendar, GitFork, Filter, FolderGit2, Check, RefreshCw, Sparkles } from 'lucide-react';

interface AnalysisFormProps {
  onSubmit: (request: AnalysisRequest) => Promise<void>;
  isLoading: boolean;
}

type FilterMode = 'TIME_BASED' | 'SELECTED_REPOS' | 'COMBINED';

export function AnalysisForm({ onSubmit, isLoading }: AnalysisFormProps) {
  const [monthsToAnalyze, setMonthsToAnalyze] = useState(6);
  const [includeForkedRepos, setIncludeForkedRepos] = useState(false);
  const [filterMode, setFilterMode] = useState<FilterMode>('TIME_BASED');
  const [selectedRepoIds, setSelectedRepoIds] = useState<string[]>([]);
  const [repositories, setRepositories] = useState<Repository[]>([]);
  const [isLoadingRepos, setIsLoadingRepos] = useState(false);
  const [showRepoSelector, setShowRepoSelector] = useState(false);

  // Load repositories when switching to selection mode
  useEffect(() => {
    if (filterMode !== 'TIME_BASED' && repositories.length === 0) {
      loadRepositories();
    }
  }, [filterMode]);

  const loadRepositories = async () => {
    setIsLoadingRepos(true);
    try {
      // First fetch fresh repos from GitHub
      const repos = await api.fetchRepositories(monthsToAnalyze, includeForkedRepos);
      setRepositories(repos);
    } catch (err) {
      // Try to get cached repos if fetch fails
      try {
        const cachedRepos = await api.getRepositories();
        setRepositories(cachedRepos);
      } catch {
        console.error('Failed to load repositories');
      }
    } finally {
      setIsLoadingRepos(false);
    }
  };

  const toggleRepoSelection = (repoId: string) => {
    setSelectedRepoIds(prev => 
      prev.includes(repoId) 
        ? prev.filter(id => id !== repoId)
        : [...prev, repoId]
    );
  };

  const selectAllRepos = () => {
    setSelectedRepoIds(repositories.map(r => r.id));
  };

  const deselectAllRepos = () => {
    setSelectedRepoIds([]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSubmit({
      monthsToAnalyze,
      includeForkedRepos,
      excludeRepos: [],
      filterMode,
      selectedRepoIds: filterMode === 'TIME_BASED' ? [] : selectedRepoIds,
    });
  };

  const canSubmit = () => {
    if (isLoading) return false;
    if (filterMode === 'TIME_BASED') return true;
    return selectedRepoIds.length > 0;
  };

  return (
    <motion.form 
      onSubmit={handleSubmit} 
      className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      <div className="flex items-center gap-2 mb-4">
        <motion.div
          animate={{ rotate: [0, 10, -10, 0] }}
          transition={{ duration: 2, repeat: Infinity }}
          className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg"
        >
          <Sparkles size={18} className="text-purple-600" />
        </motion.div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          Analysis Settings
        </h3>
      </div>

      <div className="space-y-4">
        {/* Filter Mode */}
        <div>
          <label className="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            <Filter size={16} />
            Analysis Mode
          </label>
          <div className="space-y-2">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="filterMode"
                value="TIME_BASED"
                checked={filterMode === 'TIME_BASED'}
                onChange={() => setFilterMode('TIME_BASED')}
                className="w-4 h-4 text-purple-600 focus:ring-purple-500"
              />
              <span className="text-sm text-gray-700 dark:text-gray-300">
                By Time Period (Analyze all repos)
              </span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="filterMode"
                value="SELECTED_REPOS"
                checked={filterMode === 'SELECTED_REPOS'}
                onChange={() => setFilterMode('SELECTED_REPOS')}
                className="w-4 h-4 text-purple-600 focus:ring-purple-500"
              />
              <span className="text-sm text-gray-700 dark:text-gray-300">
                Select Specific Repos
              </span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="filterMode"
                value="COMBINED"
                checked={filterMode === 'COMBINED'}
                onChange={() => setFilterMode('COMBINED')}
                className="w-4 h-4 text-purple-600 focus:ring-purple-500"
              />
              <span className="text-sm text-gray-700 dark:text-gray-300">
                Combined (Time + Selection)
              </span>
            </label>
          </div>
        </div>

        {/* Time Range - show for TIME_BASED and COMBINED */}
        {(filterMode === 'TIME_BASED' || filterMode === 'COMBINED') && (
          <div>
            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              <Calendar size={16} />
              Time Range
            </label>
            <select
              value={monthsToAnalyze}
              onChange={(e) => setMonthsToAnalyze(Number(e.target.value))}
              className="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
            >
              <option value={3}>Last 3 months</option>
              <option value={6}>Last 6 months</option>
              <option value={12}>Last 12 months</option>
              <option value={24}>Last 2 years</option>
              <option value={60}>Last 5 years</option>
            </select>
          </div>
        )}

        {/* Repository Selector - show for SELECTED_REPOS and COMBINED */}
        {filterMode !== 'TIME_BASED' && (
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                <FolderGit2 size={16} />
                Select Repositories ({selectedRepoIds.length} selected)
              </label>
              <button
                type="button"
                onClick={loadRepositories}
                disabled={isLoadingRepos}
                className="text-purple-600 hover:text-purple-700 text-sm flex items-center gap-1"
              >
                <RefreshCw size={14} className={isLoadingRepos ? 'animate-spin' : ''} />
                Refresh
              </button>
            </div>
            
            {isLoadingRepos ? (
              <div className="flex items-center justify-center py-8 border border-gray-200 dark:border-gray-700 rounded-lg">
                <Loader2 className="w-6 h-6 animate-spin text-purple-600" />
                <span className="ml-2 text-gray-600 dark:text-gray-400">Loading repositories...</span>
              </div>
            ) : repositories.length === 0 ? (
              <div className="text-center py-8 border border-gray-200 dark:border-gray-700 rounded-lg">
                <p className="text-gray-500 dark:text-gray-400 mb-2">No repositories found</p>
                <button
                  type="button"
                  onClick={loadRepositories}
                  className="text-purple-600 hover:text-purple-700 text-sm"
                >
                  Click to fetch repositories
                </button>
              </div>
            ) : (
              <div className="border border-gray-200 dark:border-gray-700 rounded-lg">
                {/* Select/Deselect All */}
                <div className="flex items-center justify-between px-3 py-2 bg-gray-50 dark:bg-gray-700/50 border-b border-gray-200 dark:border-gray-700">
                  <span className="text-xs text-gray-500 dark:text-gray-400">
                    {repositories.length} repositories available
                  </span>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={selectAllRepos}
                      className="text-xs text-purple-600 hover:text-purple-700"
                    >
                      Select All
                    </button>
                    <span className="text-gray-300 dark:text-gray-600">|</span>
                    <button
                      type="button"
                      onClick={deselectAllRepos}
                      className="text-xs text-gray-500 hover:text-gray-700 dark:hover:text-gray-300"
                    >
                      Clear
                    </button>
                  </div>
                </div>
                
                {/* Repository List */}
                <div className="max-h-64 overflow-y-auto">
                  {repositories.map(repo => (
                    <label
                      key={repo.id}
                      className={`flex items-center gap-3 px-3 py-2 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/30 border-b border-gray-100 dark:border-gray-700 last:border-b-0 ${
                        selectedRepoIds.includes(repo.id) ? 'bg-purple-50 dark:bg-purple-900/20' : ''
                      }`}
                    >
                      <div className={`w-5 h-5 rounded border flex items-center justify-center transition-colors ${
                        selectedRepoIds.includes(repo.id) 
                          ? 'bg-purple-600 border-purple-600' 
                          : 'border-gray-300 dark:border-gray-600'
                      }`}>
                        {selectedRepoIds.includes(repo.id) && (
                          <Check size={14} className="text-white" />
                        )}
                      </div>
                      <input
                        type="checkbox"
                        checked={selectedRepoIds.includes(repo.id)}
                        onChange={() => toggleRepoSelection(repo.id)}
                        className="sr-only"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-sm text-gray-900 dark:text-white truncate">
                            {repo.name}
                          </span>
                          {repo.language && (
                            <span className="px-1.5 py-0.5 text-xs bg-gray-100 dark:bg-gray-700 rounded text-gray-600 dark:text-gray-400">
                              {repo.language}
                            </span>
                          )}
                        </div>
                        {repo.description && (
                          <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                            {repo.description}
                          </p>
                        )}
                      </div>
                      <div className="flex items-center gap-2 text-xs text-gray-400">
                        <span>⭐ {repo.stars}</span>
                      </div>
                    </label>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Include Forked Repos */}
        <div className="flex items-center gap-3">
          <input
            type="checkbox"
            id="includeForked"
            checked={includeForkedRepos}
            onChange={(e) => setIncludeForkedRepos(e.target.checked)}
            className="w-4 h-4 text-purple-600 rounded focus:ring-purple-500"
          />
          <label htmlFor="includeForked" className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
            <GitFork size={16} />
            Include forked repositories
          </label>
        </div>

        {/* Submit Button */}
        <motion.button
          type="submit"
          disabled={!canSubmit()}
          whileHover={{ scale: canSubmit() ? 1.02 : 1 }}
          whileTap={{ scale: canSubmit() ? 0.98 : 1 }}
          className="w-full bg-gradient-to-r from-purple-600 to-indigo-600 text-white py-3 px-4 rounded-lg hover:from-purple-700 hover:to-indigo-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 font-medium shadow-lg shadow-purple-500/25"
        >
          {isLoading ? (
            <>
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
              >
                <Loader2 size={18} />
              </motion.div>
              Analyzing your repositories...
            </>
          ) : (
            <>
              <motion.div
                animate={{ x: [0, 3, 0] }}
                transition={{ duration: 1.5, repeat: Infinity }}
              >
                <Play size={18} />
              </motion.div>
              Run Analysis
              {filterMode !== 'TIME_BASED' && selectedRepoIds.length > 0 && (
                <span className="text-purple-200">({selectedRepoIds.length} repos)</span>
              )}
            </>
          )}
        </motion.button>
      </div>

      <AnimatePresence>
        {isLoading && (
          <motion.p 
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="text-sm text-gray-500 dark:text-gray-400 mt-3 text-center"
          >
            This may take a moment. We're extracting skills from your READMEs.
          </motion.p>
        )}
      </AnimatePresence>
      
      <AnimatePresence>
        {filterMode !== 'TIME_BASED' && selectedRepoIds.length === 0 && !isLoading && (
          <motion.p 
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="text-sm text-amber-600 dark:text-amber-400 mt-3 text-center"
          >
            Please select at least one repository to analyze.
          </motion.p>
        )}
      </AnimatePresence>
    </motion.form>
  );
}
