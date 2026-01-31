'use client';

import { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { Loader2 } from 'lucide-react';

function GitHubCallbackContent() {
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const searchParams = useSearchParams();
  const { handleGitHubCallback } = useAuth();

  useEffect(() => {
    const code = searchParams.get('code');
    
    if (code) {
      handleGitHubCallback(code)
        .then(() => {
          router.push('/dashboard');
        })
        .catch((err) => {
          setError(err.message || 'Failed to authenticate with GitHub');
        });
    } else {
      // Use setTimeout to avoid synchronous setState in effect
      setTimeout(() => {
        setError('No authorization code received from GitHub');
      }, 0);
    }
  }, [searchParams, handleGitHubCallback, router]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="bg-white dark:bg-gray-800 rounded-xl p-8 shadow-lg max-w-md text-center">
          <div className="text-red-500 text-5xl mb-4">⚠️</div>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
            Authentication Failed
          </h2>
          <p className="text-gray-600 dark:text-gray-400 mb-4">{error}</p>
          <button
            onClick={() => router.push('/login')}
            className="bg-purple-600 text-white px-6 py-2 rounded-lg hover:bg-purple-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <Loader2 className="w-12 h-12 animate-spin text-purple-600 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
          Authenticating with GitHub...
        </h2>
        <p className="text-gray-600 dark:text-gray-400 mt-2">
          Please wait while we complete the login process.
        </p>
      </div>
    </div>
  );
}

export default function GitHubCallbackPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="w-12 h-12 animate-spin text-purple-600" />
      </div>
    }>
      <GitHubCallbackContent />
    </Suspense>
  );
}
