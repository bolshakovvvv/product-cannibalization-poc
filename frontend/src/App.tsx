import { useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type ClientCard = {
  clientId: string
  snapshotId: string
  asOfAt: string
}

function App() {
  const [clientId, setClientId] = useState('')
  const [client, setClient] = useState<ClientCard | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function findClient(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const id = clientId.trim()
    if (!id || loading) return

    setLoading(true)
    setClient(null)
    setError('')

    try {
      const response = await fetch(
          `/api/clients/${encodeURIComponent(id)}`,
          {
            headers: { Accept: 'application/json' },
            signal: AbortSignal.timeout(15000),
          },
      )

      if (response.status === 404) {
        setError('Данные клиента не найдены. Проверьте ID.')
        return
      }

      if (!response.ok) {
        setError('Не удалось получить данные. Попробуйте ещё раз.')
        return
      }

      const data: ClientCard = await response.json()
      setClient(data)
    } catch {
      setError('Сервер не ответил. Попробуйте повторить поиск.')
    } finally {
      setLoading(false)
    }
  }

  return (
      <main className="workspace">
        <header className="workspace-header">
          <div>
            <span className="badge">Учебный прототип</span>
            <h1>Подбор аудитории</h1>
            <p className="subtitle">
              Онкострахование · Данные клиента и история коммуникаций
            </p>
          </div>
        </header>

        <section className="panel" aria-labelledby="search-title">
          <h2 id="search-title">Поиск клиента</h2>

          <form onSubmit={findClient}>
            <label htmlFor="client-id">ID клиента</label>

            <div className="search-row">
              <input
                  id="client-id"
                  placeholder="Введите ID клиента"
                  value={clientId}
                  maxLength={100}
                  autoComplete="off"
                  disabled={loading}
                  required
                  onChange={(event) => {
                    setClientId(event.target.value)
                    setClient(null)
                    setError('')
                  }}
              />

              <button
                  type="submit"
                  disabled={loading || !clientId.trim()}
              >
                {loading ? 'Ищем…' : 'Найти клиента'}
              </button>
            </div>
          </form>

          {loading && (
              <p className="hint" role="status">
                Получаем данные клиента…
              </p>
          )}

          {error && (
              <p className="error" role="alert">
                {error}
              </p>
          )}
        </section>

        {client && (
            <div className="dashboard">
              <div>
                <section className="panel" aria-labelledby="client-title">
                  <div className="section-heading">
                    <div className="client-heading">
                      <div className="avatar" aria-hidden="true">К</div>
                      <div>
                        <p className="eyebrow">Клиент</p>
                        <h2 id="client-title">{client.clientId}</h2>
                      </div>
                    </div>
                    <span className="neutral-tag">Найден</span>
                  </div>

                  <p className="data-date">
                    Данные на{' '}
                    {new Date(client.asOfAt).toLocaleString('ru-RU', {
                      timeZone: 'Europe/Moscow',
                    })}{' '}
                    (МСК)
                  </p>

                  <h3>Профиль клиента</h3>
                  <p className="empty-note">
                    Дополнительные сведения пока не доступны.
                  </p>

                  <h3>Страховые продукты</h3>
                  <p className="empty-note">
                    Сведения о страховых продуктах пока не доступны.
                  </p>
                </section>

                <section className="panel">
                  <h2>История коммуникаций</h2>
                  <p className="empty-note">
                    Сведения о коммуникациях пока не доступны.
                  </p>
                </section>
              </div>

              <aside>
                <section className="panel prediction-panel">
                  <span className="eyebrow">Оценка покупки</span>
                  <h2>Прогноз модели</h2>

                  <div className="prediction-placeholder">
                    <span className="prediction-number">—</span>
                    <span>Прогноз не рассчитан</span>
                  </div>

                  <p className="prediction-description">
                    После расчёта здесь появятся вероятность покупки
                    и период прогноза.
                  </p>

                  <button
                      className="full-width"
                      type="button"
                      disabled
                      aria-describedby="model-status"
                  >
                    Рассчитать прогноз
                  </button>

                  <p className="hint" id="model-status">
                    Расчёт пока недоступен
                  </p>
                </section>
              </aside>
            </div>
        )}

        {!client && !loading && !error && (
            <div className="welcome-state">
              <h2>Начните с поиска клиента</h2>
              <p>Введите ID, чтобы загрузить доступные сведения.</p>
            </div>
        )}
      </main>
  )
}

export default App