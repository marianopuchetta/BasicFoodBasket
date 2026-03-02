import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function SignUp() {
  const { t } = useTranslation();

  return (
    <main className="flex-1 bg-gray-50 flex items-center justify-center py-12 px-4">
      <div className="w-full max-w-md bg-white rounded-xl shadow-sm border border-gray-100 p-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-1">{t('signup.title')}</h2>
        <p className="text-gray-500 text-sm mb-6">{t('signup.subtitle')}</p>

        <form className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('signup.name')}</label>
            <input
              type="text"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Juan García"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('signup.email')}</label>
            <input
              type="email"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="email@ejemplo.com"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('signup.password')}</label>
            <input
              type="password"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="••••••••"
            />
          </div>
          <button
            type="submit"
            className="w-full bg-blue-700 text-white font-semibold rounded-lg py-2 hover:bg-blue-800 transition-colors mt-2"
          >
            {t('signup.submit')}
          </button>
        </form>

        <p className="text-sm text-gray-500 text-center mt-6">
          {t('signup.have_account')}{' '}
          <Link to="/login" className="text-blue-700 font-medium hover:underline">
            {t('signup.login_link')}
          </Link>
        </p>
      </div>
    </main>
  );
}
