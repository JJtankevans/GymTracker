package com.vitoroliveira.gymtracker.domain

/** Regras da foto de perfil, compartilhadas entre o app (que prepara a imagem) e o repositório/ViewModel (que valida e envia). */
object AvatarRules {
    /** Bucket público do Supabase Storage (ver docs/supabase/007_foto_de_perfil_storage.sql). */
    const val BUCKET = "user_profile_images"
    /** Lado do quadrado final em pixels: sobra para qualquer tela de perfil e fica em ~50-150 KB como JPEG. */
    const val SIZE_PX = 512
    const val JPEG_QUALITY = 85
    /** Mesmo limite configurado no bucket (2 MB); acima disso o servidor recusaria. */
    const val MAX_BYTES = 2 * 1024 * 1024
}
