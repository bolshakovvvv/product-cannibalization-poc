import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import './App.css'

type ClientCard = {
  clientId: string
  snapshotId: string
  asOfAt: string
  profile: {
    age: number | null
    gender: string | null
    region: string | null
    segment: string | null
    payrollClient: boolean | null
    bankTenureMonths: number | null
    familyOrChildren: boolean | null
  }
  insurance: {
    activeDms: boolean | null
    activeAccidentPolicy: boolean | null
    activePolicyCount: number | null
    hadInsurancePolicy: boolean | null
    daysSinceDmsPurchase: number | null
  }
  communications: {
    oncoPushes30d: number | null
    allProductPushes7d: number | null
    allProductPushes30d: number | null
    daysSinceOncoPush: number | null
    daysSinceDmsPush: number | null
    openRate180d: number | null
    ctr180d: number | null
    previousPushProduct: string | null
  }
  spending: {
    medicalSpend90d: number | null
    pharmacySpend90d: number | null
    avgMonthlySpend: number | null
  }
}

const moneyFormatter = new Intl.NumberFormat('ru-RU', {
  style: 'currency',
  currency: 'RUB',
  maximumFractionDigits: 2,
})

const percentFormatter = new Intl.NumberFormat('ru-RU', {
  style: 'percent',
  maximumFractionDigits: 1,
})

function yesNo(value: boolean | null | undefined): string {
  if (value == null) return 'Нет данных'
  return value ? 'Да' : 'Нет'
}

function money(value: number | null | undefined): string {
  return value == null ? 'Нет данных' : moneyFormatter.format(value)
}

function percent(value: number | null | undefined): string {
  return value == null ? 'Нет данных' : percentFormatter.format(value)
}

function Field({
                 label,
                 value,
               }: {
  label: string
  value: ReactNode
}) {
  return (
      <div className="detail-field">
        <dt>{label}</dt>
        <dd>{value ?? 'Нет данных'}</dd>
      </div>
  )
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
          <span className="badge">
            Учебный прототип · Синтетические данные
          </span>
            <h1>Подбор аудитории</h1>
            <p className="subtitle">
              Онкострахование · Данные клиента и сводка коммуникаций
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
                  <dl className="details-grid">
                    <Field label="Возраст, лет" value={client.profile?.age} />
                    <Field label="Пол" value={client.profile?.gender} />
                    <Field label="Регион" value={client.profile?.region} />
                    <Field label="Сегмент" value={client.profile?.segment} />
                    <Field
                        label="Стаж в банке, месяцев"
                        value={client.profile?.bankTenureMonths}
                    />
                    <Field
                        label="Зарплатный клиент"
                        value={yesNo(client.profile?.payrollClient)}
                    />
                    <Field
                        label="Есть семья или дети"
                        value={yesNo(client.profile?.familyOrChildren)}
                    />
                  </dl>

                  <h3>Страховые продукты</h3>
                  <dl className="details-grid">
                    <Field
                        label="Активный ДМС"
                        value={yesNo(client.insurance?.activeDms)}
                    />
                    <Field
                        label="Активная страховка от несчастных случаев"
                        value={yesNo(client.insurance?.activeAccidentPolicy)}
                    />
                    <Field
                        label="Активных полисов"
                        value={client.insurance?.activePolicyCount}
                    />
                    <Field
                        label="Ранее покупал страховку"
                        value={yesNo(client.insurance?.hadInsurancePolicy)}
                    />
                    <Field
                        label="Дней с покупки ДМС"
                        value={client.insurance?.daysSinceDmsPurchase}
                    />
                  </dl>
                </section>

                <section className="panel" aria-labelledby="communications-title">
                  <h2 id="communications-title">Сводка коммуникаций</h2>
                  <p className="card-caption">
                    Показатели рассчитаны на дату среза.
                  </p>

                  <div className="client-metrics">
                    <div className="client-metric">
                  <span className="client-metric-value">
                    {client.communications?.oncoPushes30d ?? '—'}
                  </span>
                      <span>Пушей по онко за 30 дней</span>
                    </div>
                    <div className="client-metric">
                  <span className="client-metric-value">
                    {client.communications?.allProductPushes7d ?? '—'}
                  </span>
                      <span>Всех пушей за 7 дней</span>
                    </div>
                    <div className="client-metric">
                  <span className="client-metric-value">
                    {client.communications?.allProductPushes30d ?? '—'}
                  </span>
                      <span>Всех пушей за 30 дней</span>
                    </div>
                  </div>

                  <dl className="details-grid">
                    <Field
                        label="Дней с последнего пуша по онко"
                        value={client.communications?.daysSinceOncoPush}
                    />
                    <Field
                        label="Дней с последнего пуша по ДМС"
                        value={client.communications?.daysSinceDmsPush}
                    />
                    <Field
                        label="Доля открытий за 180 дней"
                        value={percent(client.communications?.openRate180d)}
                    />
                    <Field
                        label="CTR за 180 дней"
                        value={percent(client.communications?.ctr180d)}
                    />
                    <Field
                        label="Продукт предыдущего пуша"
                        value={client.communications?.previousPushProduct}
                    />
                  </dl>
                </section>

                <section className="panel" aria-labelledby="spending-title">
                  <h2 id="spending-title">Расходы</h2>
                  <dl className="details-grid">
                    <Field
                        label="Медицинские траты за 90 дней"
                        value={money(client.spending?.medicalSpend90d)}
                    />
                    <Field
                        label="Траты в аптеках за 90 дней"
                        value={money(client.spending?.pharmacySpend90d)}
                    />
                    <Field
                        label="Средние траты в месяц"
                        value={money(client.spending?.avgMonthlySpend)}
                    />
                  </dl>
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