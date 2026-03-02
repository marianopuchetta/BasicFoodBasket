import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function Navbar() {
  const { t, i18n } = useTranslation();
  const [menuOpen, setMenuOpen] = useState(false);

  const toggleLanguage = () => {
    i18n.changeLanguage(i18n.language === 'es' ? 'en' : 'es');
  };

  return (
    <nav className="bg-white border-b border-gray-200 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo / Title */}
          <Link to="/" className="text-lg font-bold text-blue-800 tracking-tight whitespace-nowrap">
            {t('nav.title')}
          </Link>

          {/* Desktop nav */}
          <div className="hidden md:flex items-center gap-3">
            <button
              onClick={toggleLanguage}
              className="text-sm font-medium text-gray-600 border border-gray-300 rounded px-3 py-1.5 hover:bg-gray-50 transition-colors"
            >
              {i18n.language === 'es' ? 'EN' : 'ES'}
            </button>
            <Link
              to="/login"
              className="text-sm font-medium text-blue-700 border border-blue-700 rounded px-4 py-1.5 hover:bg-blue-50 transition-colors"
            >
              {t('nav.login')}
            </Link>
            <Link
              to="/signup"
              className="text-sm font-medium text-white bg-blue-700 rounded px-4 py-1.5 hover:bg-blue-800 transition-colors"
            >
              {t('nav.signup')}
            </Link>
          </div>

          {/* Mobile menu button */}
          <button
            className="md:hidden p-2 rounded text-gray-600 hover:bg-gray-100"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Toggle menu"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {menuOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>

        {/* Mobile dropdown */}
        {menuOpen && (
          <div className="md:hidden pb-4 flex flex-col gap-2">
            <button
              onClick={toggleLanguage}
              className="text-sm font-medium text-gray-600 border border-gray-300 rounded px-3 py-2 hover:bg-gray-50 transition-colors text-left"
            >
              {i18n.language === 'es' ? 'EN' : 'ES'}
            </button>
            <Link
              to="/login"
              onClick={() => setMenuOpen(false)}
              className="text-sm font-medium text-blue-700 border border-blue-700 rounded px-4 py-2 hover:bg-blue-50 transition-colors"
            >
              {t('nav.login')}
            </Link>
            <Link
              to="/signup"
              onClick={() => setMenuOpen(false)}
              className="text-sm font-medium text-white bg-blue-700 rounded px-4 py-2 hover:bg-blue-800 transition-colors text-center"
            >
              {t('nav.signup')}
            </Link>
          </div>
        )}
      </div>
    </nav>
  );
}
