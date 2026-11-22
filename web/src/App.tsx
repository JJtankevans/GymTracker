import { useEffect, useState } from 'react'
import type { Session } from '@supabase/supabase-js'
import { MOCK, supabase } from './supabase'
import { Login } from './screens/Login'
import { Home } from './screens/Home'
import { More } from './screens/More'
import { Treino } from './screens/Treino'
import { Historico } from './screens/Historico'
import { Grupos } from './screens/Grupos'

export type Tab = 'home' | 'treino' | 'historico' | 'grupos' | 'mais'

const icons: Record<Tab, string> = {
  home: 'M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10',
  treino: 'M6 6v12M3 9v6M18 6v12M21 9v6M6 12h12',
  historico: 'M3 12a9 9 0 1 0 3-6.7M3 4v5h5M12 7v5l3 2',
  grupos: 'M4 6h16M4 12h16M4 18h10',
  mais: 'M5 12h.01M12 12h.01M19 12h.01',
}
const labels: Record<Tab, string> = { home: 'Início', treino: 'Treino', historico: 'Histórico', grupos: 'Grupos', mais: 'Mais' }

export function App() {
  const [session, setSession] = useState<Session | null | undefined>(MOCK ? null : undefined)
  const [tab, setTab] = useState<Tab>('home')
  const [pending, setPending] = useState<string | null>(null)
  const goTo = (t: Tab, groupId?: string) => { if (groupId) setPending(groupId); setTab(t) }

  useEffect(() => {
    if (MOCK) return
    supabase.auth.getSession().then(({ data }) => setSession(data.session))
    const { data: sub } = supabase.auth.onAuthStateChange((_e, s) => setSession(s))
    return () => sub.subscription.unsubscribe()
  }, [])

  if (!MOCK && session === undefined) return <div className="app"><div className="scroll"><div className="page"><div className="skel" /></div></div></div>
  if (!MOCK && !session) return <Login />

  return (
    <div className="app">
      <div className="scroll">
        <div className="page">
          {tab === 'home' && <Home go={goTo} />}
          {tab === 'treino' && <Treino pending={pending} clearPending={() => setPending(null)} onDone={() => setTab('home')} />}
          {tab === 'historico' && <Historico />}
          {tab === 'grupos' && <Grupos />}
          {tab === 'mais' && <More />}
        </div>
      </div>
      <nav className="tabbar" aria-label="Seções">
        {(Object.keys(icons) as Tab[]).map((t) => (
          <button key={t} className="tab" aria-current={tab === t ? 'page' : undefined} onClick={() => setTab(t)}>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d={icons[t]} /></svg>
            {labels[t]}
          </button>
        ))}
      </nav>
    </div>
  )
}
