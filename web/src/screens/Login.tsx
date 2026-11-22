import { useState, type FormEvent } from 'react'
import { configured, supabase } from '../supabase'

export function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password })
    setBusy(false)
    if (error) setError(error.message.includes('Invalid login') ? 'E-mail ou senha incorretos.' : error.message.includes('not confirmed') ? 'Confirme seu e-mail antes de entrar.' : error.message)
  }

  return (
    <div className="app">
      <div className="scroll" style={{ display: 'flex', alignItems: 'center' }}>
        <form className="page" style={{ width: '100%' }} onSubmit={submit}>
          <div className="title" style={{ fontSize: 36 }}>GymTracker</div>
          <p className="muted" style={{ margin: 0 }}>Entre com a mesma conta do app.</p>
          {!configured && <p className="error">Faltam VITE_SUPABASE_URL e VITE_SUPABASE_KEY (veja .env.example).</p>}
          <label className="field">
            <span className="label">E-mail</span>
            <input type="email" autoComplete="email" inputMode="email" autoCapitalize="none" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label className="field">
            <span className="label">Senha</span>
            <input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
          </label>
          {error && <div className="error" role="alert">{error}</div>}
          <button className="btn primary block" disabled={busy || !email || !password}>{busy ? 'Entrando…' : 'Entrar'}</button>
          <p className="muted install">No iPhone: toque em Compartilhar e depois em “Adicionar à Tela de Início” para abrir em tela cheia.</p>
        </form>
      </div>
    </div>
  )
}
