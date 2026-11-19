package com.example.viewmodel

import com.example.network.GeminiApiService
import com.example.network.GroqApiService
import com.example.util.AiProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class AiAttemptChainTest {

    private fun ids(chain: List<com.example.util.AiModelInfo>) = chain.map { it.id }

    @Test
    fun groqSelected_triesOtherGroqModelThenGemini() {
        val chain = buildAiAttemptChain(AiProvider.GROQ, GroqApiService.MODEL_GPT_OSS_120B, true, true)
        assertEquals(
            listOf(
                GroqApiService.MODEL_GPT_OSS_120B,
                GroqApiService.MODEL_GPT_OSS_20B,
                GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE,
                GeminiApiService.MODEL_GEMINI_3_1_FLASH_LITE
            ),
            ids(chain)
        )
    }

    @Test
    fun selectedModelGoesFirstEvenWhenNotRecommended() {
        val chain = buildAiAttemptChain(AiProvider.GROQ, GroqApiService.MODEL_GPT_OSS_20B, true, true)
        assertEquals(GroqApiService.MODEL_GPT_OSS_20B, chain.first().id)
        assertEquals(GroqApiService.MODEL_GPT_OSS_120B, chain[1].id)
    }

    @Test
    fun geminiSelected_triesOtherGeminiModelThenGroq() {
        val chain = buildAiAttemptChain(AiProvider.GEMINI, GeminiApiService.MODEL_GEMINI_3_1_FLASH_LITE, true, true)
        assertEquals(
            listOf(
                GeminiApiService.MODEL_GEMINI_3_1_FLASH_LITE,
                GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE,
                GroqApiService.MODEL_GPT_OSS_120B,
                GroqApiService.MODEL_GPT_OSS_20B
            ),
            ids(chain)
        )
    }

    @Test
    fun providerWithoutKeysIsSkipped() {
        val chain = buildAiAttemptChain(AiProvider.GROQ, GroqApiService.MODEL_GPT_OSS_120B, true, false)
        assertEquals(
            listOf(GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE, GeminiApiService.MODEL_GEMINI_3_1_FLASH_LITE),
            ids(chain)
        )
    }
}
