package com.tangem.feature.onboarding.domain

import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.MnemonicErrorResult
import com.tangem.feature.onboarding.data.MnemonicRepository
import com.tangem.utils.extensions.isNotWhitespace

/**
 * @author Anton Zhilenkov on 17.04.2023.
 */
internal class DefaultSeedPhraseInteractor constructor(
    private val repository: MnemonicRepository,
) : SeedPhraseInteractor {

    private var currentMnemonic: Mnemonic? = null
    private val partWordFinder: PartWordFinder = PartWordFinder()

    override suspend fun generateMnemonic(): Result<Mnemonic> {
        return runCatching {
            repository.generateDefaultMnemonic().apply {
                currentMnemonic = this
            }
        }
    }

    override suspend fun getMnemonicComponents(): Result<List<String>> {
        return runCatching {
            val mnemonic = currentMnemonic ?: repository.generateDefaultMnemonic().apply {
                currentMnemonic = this
            }
            mnemonic.mnemonicComponents
        }
    }

    override suspend fun isWordMatch(word: String): Boolean {
        return repository.getWordsDictionary().contains(word)
    }

    override suspend fun validateMnemonicString(text: String): Result<List<String>> {
        val inputWords = text.split("\\s+".toRegex()).toSet()
        val wordsDictionary = repository.getWordsDictionary()

        val invalidWords = inputWords
            .filter { it.isNotEmpty() && !wordsDictionary.contains(it) }
            .toSet()

        return if (invalidWords.isNotEmpty()) {
            Result.failure(SeedPhraseError.InvalidWords(invalidWords))
        } else {
            try {
                val mnemonic = repository.createMnemonic(text)
                Result.success(mnemonic.mnemonicComponents)
            } catch (ex: TangemSdkError.MnemonicException) {
                val error = ex.mnemonicResult.mapToError()
                Result.failure(error)
            }
        }
    }

    override suspend fun getSuggestions(text: String, hasSelection: Boolean, cursorPosition: Int): List<String> {
        if (text.isEmpty() || cursorPosition == 0 || hasSelection) return emptyList()
        val word = partWordFinder.getLeadPartOfWord(text, cursorPosition)
            ?: return emptyList()

        val suggestions = repository.getWordsDictionary()
            .filter { it.startsWith(word, ignoreCase = false) && it != word }
            .toList()

        return suggestions
    }

    override suspend fun insertSuggestionWord(
        text: String,
        suggestion: String,
        cursorPosition: Int,
    ): InsertSuggestionResult {
        val textPartLead = text.substring(0, cursorPosition)
        val textPartLast = text.substring(cursorPosition, text.length)
        val wordPartLast = partWordFinder.getLastPartOfWord(text, suggestion, cursorPosition)

        val textBuilder = StringBuilder()
            .append(textPartLead)
            .append(wordPartLast)

        if (textPartLast.isEmpty() || textPartLast[0].isNotWhitespace()) {
            textBuilder.append(SeedPhraseInteractor.MNEMONIC_DELIMITER)
        }
        textBuilder.append(textPartLast)

        val textWithSuggestion = textBuilder.toString()
        val newCursorPosition = cursorPosition + wordPartLast.length + SeedPhraseInteractor.MNEMONIC_DELIMITER.length

        return InsertSuggestionResult(textWithSuggestion, newCursorPosition)
    }
}

private fun MnemonicErrorResult.mapToError(): SeedPhraseError = when (this) {
    MnemonicErrorResult.InvalidWordCount -> SeedPhraseError.InvalidEntropyLength
    MnemonicErrorResult.InvalidEntropyLength -> SeedPhraseError.InvalidEntropyLength
    MnemonicErrorResult.InvalidWordsFile -> SeedPhraseError.InvalidWordsFile
    MnemonicErrorResult.InvalidChecksum -> SeedPhraseError.InvalidChecksum
    MnemonicErrorResult.MnenmonicCreationFailed -> SeedPhraseError.MnenmonicCreationFailed
    MnemonicErrorResult.NormalizationFailed -> SeedPhraseError.NormalizationFailed
    MnemonicErrorResult.UnsupportedLanguage -> SeedPhraseError.UnsupportedLanguage
    is MnemonicErrorResult.InvalidWords -> SeedPhraseError.InvalidWords(this.words)
}
