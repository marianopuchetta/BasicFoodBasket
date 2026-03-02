import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function Home() {
  const { t } = useTranslation();

  return (
    <main className="flex-1 bg-gray-50">
      {/* Hero section */}
      <section className="bg-blue-800 text-white py-20 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <h1 className="text-4xl sm:text-5xl font-bold mb-4 leading-tight">
            {t('home.hero_title')}
          </h1>
          <p className="text-blue-100 text-lg sm:text-xl mb-8 max-w-2xl mx-auto">
            {t('home.hero_subtitle')}
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              to="/login"
              className="bg-white text-blue-800 font-semibold px-8 py-3 rounded-lg hover:bg-blue-50 transition-colors"
            >
              {t('home.cta_login')}
            </Link>
            <Link
              to="/signup"
              className="bg-blue-600 text-white font-semibold px-8 py-3 rounded-lg border border-blue-400 hover:bg-blue-700 transition-colors"
            >
              {t('home.cta_signup')}
            </Link>
          </div>
        </div>
      </section>

      {/* Features section */}
      <section className="py-16 px-4">
        <div className="max-w-5xl mx-auto grid gap-8 sm:grid-cols-3">
          {[
            { icon: '📊', title: t('nav.title') },
            { icon: '🛒', title: 'Canasta Básica' },
            { icon: '📈', title: 'Historial de Precios' },
          ].map((item) => (
            <div
              key={item.title}
              className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 text-center"
            >
              <div className="text-4xl mb-3">{item.icon}</div>
              <h3 className="text-base font-semibold text-gray-800">{item.title}</h3>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
