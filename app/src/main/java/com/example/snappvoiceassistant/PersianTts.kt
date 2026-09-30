package com.example.snappvoiceassistant

import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * برای صحبت‌کردن، موتورها را به‌ترتیب امتحان می‌کند:
 * ۱) موتور پیش‌فرض سیستم (چیزی که کاربر در تنظیمات گوشی انتخاب کرده)
 * ۲) SherpaTTS به‌صورت صریح
 * ۳) گوگل به‌صورت صریح
 * و یک گزارش متنی از نتیجهٔ هر تلاش نگه می‌دارد تا در صورت شکست، دقیقاً معلوم باشد
 * کدام مرحله و با چه کد خطایی شکست خورده.
 */
class PersianTts(
    private val context: Context,
    private val onResult: (ready: Boolean, debugLog: String) -> Unit
) {
    companion object {
        const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        const val SHERPA_TTS_PACKAGE = "org.woheller69.ttsengine"
        private val PERSIAN_LANGS = setOf("fa", "fas", "per")
    }

    var tts: TextToSpeech? = null
        private set

    private val candidates = mutableListOf<String?>()
    private var index = 0
    private val log = StringBuilder()

    fun start() {
        candidates.clear()
        log.clear()
        candidates.add(null) // اول: موتور پیش‌فرض سیستم
        if (isInstalled(SHERPA_TTS_PACKAGE)) candidates.add(SHERPA_TTS_PACKAGE)
        if (isInstalled(GOOGLE_TTS_PACKAGE)) candidates.add(GOOGLE_TTS_PACKAGE)
        log.append("موتورهای پیدا‌شده برای امتحان: ${candidates.map { it ?: "پیش‌فرض سیستم" }}\n")
        index = 0
        tryNext()
    }

    private fun tryNext() {
        tts?.shutdown()
        tts = null
        if (index >= candidates.size) {
            onResult(false, log.toString())
            return
        }
        val engine = candidates[index]
        val engineLabel = engine ?: "پیش‌فرض سیستم"
        index++
        try {
            tts = TextToSpeech(context, { status ->
                val t = tts
                if (status == TextToSpeech.SUCCESS && t != null) {
                    val connectedEngine = try { t.defaultEngine } catch (e: Exception) { "خطا: ${e.message}" }
                    log.append("[$engineLabel] وصل شد ✅ — موتور واقعی متصل‌شده: $connectedEngine\n")
                    val isTrustedEngine = engine == SHERPA_TTS_PACKAGE || connectedEngine == SHERPA_TTS_PACKAGE
                    val voiceResult = configurePersian(t)
                    log.append("[$engineLabel] پیدا‌کردن صدای فارسی: ${if (voiceResult) "موفق ✅" else "ناموفق ❌"}\n")
                    if (voiceResult || isTrustedEngine) {
                        onResult(true, log.toString())
                    } else {
                        tryNext()
                    }
                } else {
                    log.append("[$engineLabel] وصل نشد ❌ — کد وضعیت: $status\n")
                    tryNext()
                }
            }, engine)
        } catch (e: Exception) {
            log.append("[$engineLabel] خطای ساخت TTS: ${e.message}\n")
            tryNext()
        }
    }

    private fun configurePersian(t: TextToSpeech): Boolean {
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
