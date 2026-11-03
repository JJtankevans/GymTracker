// Configuração do servidor (Supabase). A chave "anon" é pública por desenho, mas mesmo assim não fica no repositório:
// o CI escreve este arquivo no deploy com os secrets SUPABASE_URL e SUPABASE_KEY. Vazio, o app pergunta na primeira vez.
window.GYM_CONFIG = { url: "", key: "" };
