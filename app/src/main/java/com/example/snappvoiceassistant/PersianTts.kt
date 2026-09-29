package com.example.snappvoiceassistant

import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * برای صحبت‌کردن، اول از همان موتور «پیش‌فرضِ کل سیستم» استفاده می‌کند — یعنی هر موتوری
 * که خودِ کاربر در تنظیمات گوشی (تبدیل متن به گفتار > موتور ترجیحی) انتخاب کرده. این
 * مطمئن‌ترین راه است چون دقیقاً همان چیزی‌ست که کاربر خودش تست و تأیید کرده.
 * اگر آن به هر دلیلی جواب نداد، به‌عنوان نسخهٔ پشتیبان، موتورهای شناخته‌شده
 * (SherpaTTS و گوگل) را صریحاً هم امتحان می‌کند.
 */
class PersianTts(
    private val context: Context,
    private val onResult: (ready: Boolean) -> Unit
) {
    companion object {
        const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        const val SHERPA_TTS_PACKAGE = "org.woheller69.ttsengine"
        private val PERSIAN_LANGS = setOf("fa", "fas", "per")
    }

    var tts: TextToSpeech? = null
        private set

    // null یعنی «موتور پیش‌فرض سیستم» (همانی که در تنظیمات گوشی انتخاب شده)
    private val candidates = mutableListOf<String?>(null)
    private var index = 0

    fun start() {
        candidates.clear()
        candidates.add(null) // اول: موتور پیش‌فرض سیستم (چیزی که کاربر در تنظیمات انتخاب کرده)
        if (isInstalled(SHERPA_TTS_PACKAGE)) candidates.add(SHERPA_TTS_PACKAGE)
        if (isInstalled(GOOGLE_TTS_PACKAGE)) candidates.add(GOOGLE_TTS_PACKAGE)
        index = 0
        tryNext()
    }

    private fun tryNext() {
        tts?.shutdown()
        tts = null
        if (index >= candidates.size) {
            onResult(false)
            return
        }
        val engine = candidates[index++]
        tts = TextToSpeech(context, { status ->
            val t = tts
            if (status == TextToSpeech.SUCCESS && t != null) {
                // موتوری که واقعاً وصل شده را (چه صریح خواسته باشیم، چه پیش‌فرض بوده) بررسی کن
                val connectedEngine = try { t.defaultEngine } catch (e: Exception) { null }
                val isTrustedEngine = engine == SHERPA_TTS_PACKAGE || connectedEngine == SHERPA_TTS_PACKAGE
                if (configurePersian(t) || isTrustedEngine) {
                    onResult(true)
                } else {
                    tryNext()
                }
            } else {
                tryNext()
            }
        }, engine)
    }

    private fun configurePersian(t: TextToSpeech): Boolean {
        // ۱) دنبال صدایی بگرد که زبانش فارسی باشد (با هر کد زبانی)
        try {
            val voice = t.voices?.firstOrNull { v ->
                v.locale.language.lowercase() in PERSIAN_LANGS &&
                        !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            if (voice != null) {
                t.voice = voice
                return true
            }
        } catch (e: Exception) {
            // ادامه با روش دوم
        }
        // ۲) روش دوم: امتحان کردن کدهای زبانِ ممکن
        val locales = listOf(
            Locale("fa", "IR"), Locale("fa"),
            Locale("fas", "IR"), Locale("fas"),
            Locale("per")
        )
        for (l in locales) {
            try {
                if (t.isLanguageAvailable(l) >= TextToSpeech.LANG_AVAILABLE) {
                    t.language = l
                    return true
                }
            } catch (e: Exception) {
                // این کد را رد کن
            }
        }
        return false
    }

    private fun isInstalled(pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
