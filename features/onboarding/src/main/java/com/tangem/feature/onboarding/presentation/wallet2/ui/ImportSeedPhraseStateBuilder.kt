package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.onboarding.domain.SeedPhraseError
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState

/**
 * @author Anton Zhilenkov on 16.04.2023.
 */
class ImportSeedPhraseStateBuilder {

    fun updateTextField(
        uiState: OnboardingSeedPhraseState,
        textFieldValue: TextFieldValue,
    ): OnboardingSeedPhraseState = uiState.copy(
        importSeedPhraseState = uiState.importSeedPhraseState.copy(
            tvSeedPhrase = uiState.importSeedPhraseState.tvSeedPhrase.copy(
                textFieldValue = textFieldValue,
            ),
        ),
    )

    fun updateCreateWalletButton(
        uiState: OnboardingSeedPhraseState,
        enabled: Boolean,
    ): OnboardingSeedPhraseState {
        return uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                buttonCreateWallet = uiState.importSeedPhraseState.buttonCreateWallet.copy(
                    enabled = enabled,
                ),
            ),
        )
    }

    fun updateInvalidWords(
        uiState: OnboardingSeedPhraseState,
        invalidWords: Set<String>,
    ): OnboardingSeedPhraseState = uiState.copy(
        importSeedPhraseState = uiState.importSeedPhraseState.copy(
            invalidWords = invalidWords,
        ),
    )

    fun updateError(
        uiState: OnboardingSeedPhraseState,
        error: SeedPhraseError?,
    ): OnboardingSeedPhraseState {
        return uiState.copy(
            importSeedPhraseState = uiState.importSeedPhraseState.copy(
                error = error,
            ),
        )
    }
}
