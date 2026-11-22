import { MOCK, supabase } from '../supabase'

export function More() {
  return (
    <>
      <div className="title">Mais</div>
      <div className="card row">
        <div className="grow">
          <div style={{ fontWeight: 700 }}>Conta</div>
          <div className="muted">{MOCK ? 'Modo demonstração (dados falsos)' : 'Conectado ao Supabase'}</div>
        </div>
        {!MOCK && <button className="btn" onClick={() => supabase.auth.signOut()}>Sair</button>}
      </div>
      <div className="card muted install">Evolução, recordes e conquistas chegam nas próximas rodadas.</div>
    </>
  )
}
