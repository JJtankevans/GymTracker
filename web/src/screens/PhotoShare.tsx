import { useRef, useState } from 'react'
import { photoCardImage, shareBlob, type CardSpec } from '../shareCard'

/** "Foto com estatísticas": abre a câmera (ou a galeria), mostra a prévia com os números por cima e compartilha. */
export function PhotoShare({ spec, fileName = 'gymtracker_foto.png' }: { spec: CardSpec; fileName?: string }) {
  const input = useRef<HTMLInputElement>(null)
  const [blob, setBlob] = useState<Blob | null>(null)
  const [url, setUrl] = useState<string | null>(null)
  const [msg, setMsg] = useState('')

  const close = () => { if (url) URL.revokeObjectURL(url); setBlob(null); setUrl(null) }
  const onFile = async (f: File | undefined) => {
    if (!f) return
    setMsg('')
    try {
      const b = await photoCardImage(f, spec)
      setBlob(b); setUrl(URL.createObjectURL(b))
    } catch { setMsg('Não consegui abrir essa foto.') }
    if (input.current) input.current.value = ''
  }
  const share = async () => {
    if (!blob) return
    try { await shareBlob(blob, fileName, spec.title); close() } catch (e) { if ((e as Error).name !== 'AbortError') setMsg((e as Error).message) }
  }

  return (
    <>
      <button className="btn block" onClick={() => input.current?.click()}>Foto com estatísticas</button>
      <input ref={input} type="file" accept="image/*" capture="environment" hidden onChange={(e) => onFile(e.target.files?.[0])} />
      {msg && <p className="error">{msg}</p>}
      {url && (
        <div role="dialog" aria-modal="true" aria-label="Prévia da foto com as estatísticas"
          style={{ position: 'fixed', inset: 0, zIndex: 50, background: 'rgba(0,0,0,0.85)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12, padding: 16 }}>
          <img src={url} alt="Foto com as estatísticas do treino" style={{ maxWidth: '100%', maxHeight: '72vh', borderRadius: 12 }} />
          <div className="row" style={{ gap: 8, width: '100%', maxWidth: 420 }}>
            <button className="btn grow" onClick={close}>Descartar</button>
            <button className="btn primary grow" onClick={share}>Compartilhar</button>
          </div>
        </div>
      )}
    </>
  )
}
