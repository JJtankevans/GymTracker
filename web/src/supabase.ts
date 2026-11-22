import { createClient } from '@supabase/supabase-js'

const url = import.meta.env.VITE_SUPABASE_URL as string | undefined
const key = import.meta.env.VITE_SUPABASE_KEY as string | undefined

/** Modo de demonstração (dados falsos em memória): ?mock na URL ou VITE_MOCK=1. Serve para ver a interface sem login. */
export const MOCK = typeof location !== 'undefined' && (new URLSearchParams(location.search).has('mock') || import.meta.env.VITE_MOCK === '1')
export const configured = !!url && !!key

export const supabase = createClient(url ?? 'http://localhost', key ?? 'missing', {
  auth: { persistSession: true, autoRefreshToken: true, storageKey: 'gymtracker-web-auth' },
})
