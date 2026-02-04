'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { AnalysisResult } from '@/lib/types';
import { 
  ArrowLeft,
  Trash2,
  AlertTriangle,
  Loader2,
  User,
  Clock,
  FileText,
  Shield,
  X,
  Check
} from 'lucide-react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'framer-motion';

export default function SettingsPage() {
  const { user, isAuthenticated, isLoading: authLoading, logout } = useAuth();
  const router = useRouter();
  
  const [analysisHistory, setAnalysisHistory] = useState<AnalysisResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const [isDeletingAnalysis, setIsDeletingAnalysis] = useState<string | null>(null);
  const [isDeletingAllAnalyses, setIsDeletingAllAnalyses] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  
  const [showDeleteAccountModal, setShowDeleteAccountModal] = useState(false);
  const [showDeleteAllAnalysesModal, setShowDeleteAllAnalysesModal] = useState(false);
  const [confirmText, setConfirmText] = useState('');

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (isAuthenticated) {
      loadAnalysisHistory();
    }
  }, [isAuthenticated]);

  const loadAnalysisHistory = async () => {
    setIsLoading(true);
    try {
      const history = await api.getAnalysisHistory();
      setAnalysisHistory(history || []);
    } catch (err) {
      console.error('Failed to load analysis history:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteAnalysis = async (analysisId: string) => {
    setIsDeletingAnalysis(analysisId);
    setError(null);
    try {
      await api.deleteAnalysis(analysisId);
      setAnalysisHistory(prev => prev.filter(a => a.analysisId !== analysisId));
      setSuccess('Analysis deleted successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete analysis');
    } finally {
      setIsDeletingAnalysis(null);
    }
  };

  const handleDeleteAllAnalyses = async () => {
    if (confirmText !== 'DELETE ALL') return;
    
    setIsDeletingAllAnalyses(true);
    setError(null);
    try {
      await api.deleteAllAnalyses();
      setAnalysisHistory([]);
      setShowDeleteAllAnalysesModal(false);
      setConfirmText('');
      setSuccess('All analyses deleted successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete analyses');
    } finally {
      setIsDeletingAllAnalyses(false);
    }
  };

  const handleDeleteAccount = async () => {
    if (confirmText !== 'DELETE ACCOUNT') return;
    
    setIsDeletingAccount(true);
    setError(null);
    try {
      await api.deleteAccount();
      logout();
      router.push('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete account');
      setIsDeletingAccount(false);
    }
  };

  if (authLoading || isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin text-purple-600 mx-auto mb-4" />
          <p className="text-gray-600 dark:text-gray-400">Loading settings...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <Link 
          href="/dashboard"
          className="inline-flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-4"
        >
          <ArrowLeft size={16} />
          Back to Dashboard
        </Link>
        
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
          Settings
        </h1>
        <p className="text-gray-600 dark:text-gray-400 mt-2">
          Manage your account and data
        </p>
      </div>

      {/* Success Message */}
      <AnimatePresence>
        {success && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 rounded-xl p-4 mb-6 flex items-center gap-3"
          >
            <Check className="text-green-600" size={20} />
            <p className="text-green-700 dark:text-green-400">{success}</p>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-xl p-4 mb-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertTriangle className="text-red-600" size={20} />
            <p className="text-red-700 dark:text-red-400">{error}</p>
          </div>
          <button onClick={() => setError(null)}>
            <X size={18} className="text-red-500" />
          </button>
        </div>
      )}

      {/* Account Info */}
      <section className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
          <User size={20} className="text-purple-500" />
          Account Information
        </h2>
        <div className="space-y-3">
          <div className="flex justify-between py-2 border-b border-gray-100 dark:border-gray-700">
            <span className="text-gray-600 dark:text-gray-400">Name</span>
            <span className="font-medium text-gray-900 dark:text-white">{user?.name}</span>
          </div>
          <div className="flex justify-between py-2 border-b border-gray-100 dark:border-gray-700">
            <span className="text-gray-600 dark:text-gray-400">Email</span>
            <span className="font-medium text-gray-900 dark:text-white">{user?.email}</span>
          </div>
          <div className="flex justify-between py-2 border-b border-gray-100 dark:border-gray-700">
            <span className="text-gray-600 dark:text-gray-400">GitHub</span>
            <span className="font-medium text-gray-900 dark:text-white">
              {user?.githubUsername ? `@${user.githubUsername}` : 'Not linked'}
            </span>
          </div>
          <div className="flex justify-between py-2">
            <span className="text-gray-600 dark:text-gray-400">Member since</span>
            <span className="font-medium text-gray-900 dark:text-white">
              {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
            </span>
          </div>
        </div>
      </section>

      {/* Analysis History */}
      <section className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-sm border border-gray-200 dark:border-gray-700 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <FileText size={20} className="text-blue-500" />
            Analysis History
          </h2>
          {analysisHistory.length > 0 && (
            <button
              onClick={() => setShowDeleteAllAnalysesModal(true)}
              className="text-sm text-red-600 hover:text-red-700 flex items-center gap-1"
            >
              <Trash2 size={14} />
              Delete All
            </button>
          )}
        </div>
        
        {analysisHistory.length === 0 ? (
          <p className="text-gray-500 dark:text-gray-400 text-center py-8">
            No analyses yet. Run your first analysis to see it here.
          </p>
        ) : (
          <div className="space-y-3">
            {analysisHistory.map((analysis) => (
              <div
                key={analysis.analysisId}
                className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
              >
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 bg-purple-100 dark:bg-purple-900/50 rounded-lg flex items-center justify-center">
                    <FileText className="text-purple-600" size={18} />
                  </div>
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">
                      Analysis #{analysis.analysisId?.slice(-6) || 'N/A'}
                    </p>
                    <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                      <Clock size={14} />
                      <span>
                        {analysis.analyzedAt 
                          ? new Date(analysis.analyzedAt).toLocaleString() 
                          : 'Unknown date'}
                      </span>
                      <span>•</span>
                      <span>{analysis.repositoriesAnalyzed || 0} repos</span>
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => handleDeleteAnalysis(analysis.analysisId!)}
                  disabled={isDeletingAnalysis === analysis.analysisId}
                  className="p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors disabled:opacity-50"
                >
                  {isDeletingAnalysis === analysis.analysisId ? (
                    <Loader2 size={18} className="animate-spin" />
                  ) : (
                    <Trash2 size={18} />
                  )}
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Danger Zone */}
      <section className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl p-6">
        <h2 className="text-lg font-semibold text-red-700 dark:text-red-400 mb-4 flex items-center gap-2">
          <Shield size={20} />
          Danger Zone
        </h2>
        <p className="text-red-600 dark:text-red-300 text-sm mb-4">
          These actions are irreversible. Please proceed with caution.
        </p>
        <button
          onClick={() => setShowDeleteAccountModal(true)}
          className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg font-medium transition-colors"
        >
          <Trash2 size={18} />
          Delete My Account
        </button>
      </section>

      {/* Delete All Analyses Modal */}
      <AnimatePresence>
        {showDeleteAllAnalysesModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setShowDeleteAllAnalysesModal(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-md w-full shadow-xl"
              onClick={e => e.stopPropagation()}
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 bg-red-100 dark:bg-red-900/50 rounded-full flex items-center justify-center">
                  <AlertTriangle className="text-red-600" size={24} />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                    Delete All Analyses
                  </h3>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    This action cannot be undone
                  </p>
                </div>
              </div>
              
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                This will permanently delete all {analysisHistory.length} analyses and their associated data.
              </p>
              
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">
                Type <strong>DELETE ALL</strong> to confirm:
              </p>
              <input
                type="text"
                value={confirmText}
                onChange={e => setConfirmText(e.target.value)}
                placeholder="DELETE ALL"
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white mb-4"
              />
              
              <div className="flex gap-3">
                <button
                  onClick={() => {
                    setShowDeleteAllAnalysesModal(false);
                    setConfirmText('');
                  }}
                  className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleDeleteAllAnalyses}
                  disabled={confirmText !== 'DELETE ALL' || isDeletingAllAnalyses}
                  className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {isDeletingAllAnalyses ? (
                    <Loader2 size={18} className="animate-spin" />
                  ) : (
                    <Trash2 size={18} />
                  )}
                  Delete All
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Delete Account Modal */}
      <AnimatePresence>
        {showDeleteAccountModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setShowDeleteAccountModal(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-md w-full shadow-xl"
              onClick={e => e.stopPropagation()}
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 bg-red-100 dark:bg-red-900/50 rounded-full flex items-center justify-center">
                  <AlertTriangle className="text-red-600" size={24} />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                    Delete Account
                  </h3>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    This action cannot be undone
                  </p>
                </div>
              </div>
              
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                This will permanently delete your account and all associated data including:
              </p>
              <ul className="text-sm text-gray-600 dark:text-gray-400 mb-4 space-y-1 ml-4 list-disc">
                <li>All your analyses</li>
                <li>Skill profiles and progression data</li>
                <li>Role inferences and recommendations</li>
                <li>Repository data and summaries</li>
              </ul>
              
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">
                Type <strong>DELETE ACCOUNT</strong> to confirm:
              </p>
              <input
                type="text"
                value={confirmText}
                onChange={e => setConfirmText(e.target.value)}
                placeholder="DELETE ACCOUNT"
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white mb-4"
              />
              
              <div className="flex gap-3">
                <button
                  onClick={() => {
                    setShowDeleteAccountModal(false);
                    setConfirmText('');
                  }}
                  className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleDeleteAccount}
                  disabled={confirmText !== 'DELETE ACCOUNT' || isDeletingAccount}
                  className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {isDeletingAccount ? (
                    <Loader2 size={18} className="animate-spin" />
                  ) : (
                    <Trash2 size={18} />
                  )}
                  Delete Account
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
