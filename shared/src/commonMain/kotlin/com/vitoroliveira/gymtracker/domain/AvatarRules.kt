package com.vitoroliveira.gymtracker.domain

/** Regras da foto de perfil, compartilhadas entre o app (que prepara a imagem) e o repositório/ViewModel (que valida e envia). */
object AvatarRules {
    /** Bucket público do Supabase Storage (ver docs/supabase/007_foto_de_perfil_storage.sql). */
    const val BUCKET = "user_profile_images"
    /**
     * Lado do quadrado final em pixels. A maior foto na tela é a do Perfil (96 dp ≈ 290 px em telas 3x), então 384 já
     * sobra e pesa bem menos que 512 (cerca de 45% menos pixels).
     */
    const val SIZE_PX = 384
    /** Qualidade JPEG inicial. Se passar de [TARGET_BYTES], o app baixa a qualidade até caber (nunca abaixo de [MIN_JPEG_QUALITY]). */
    const val JPEG_QUALITY = 80
    const val MIN_JPEG_QUALITY = 50
    /** Meta de tamanho por foto: 1 GB do plano grátis / 100 KB = 10 mil fotos no pior caso (a maioria fica em 20-50 KB). */
    const val TARGET_BYTES = 100 * 1024
    /** Mesmo limite configurado no bucket (2 MB); acima disso o servidor recusaria. */
    const val MAX_BYTES = 2 * 1024 * 1024
}
