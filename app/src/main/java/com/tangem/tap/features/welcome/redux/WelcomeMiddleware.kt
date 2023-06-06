package com.tangem.tap.features.welcome.redux

import android.content.Intent
import com.tangem.common.*
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.scanCard.ScanCardProcessor
import com.tangem.tap.domain.userWalletList.unlockIfLockable
import com.tangem.tap.features.intentHandler.handlers.WalletConnectLinkIntentHandler
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.features.signin.redux.SignInAction
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

internal class WelcomeMiddleware {
    val middleware: Middleware<AppState> = { _, appStateProvider ->
        { next ->
            { action ->
                val appState = appStateProvider()
                if (action is WelcomeAction && appState != null) {
                    handleAction(action, appState.welcomeState)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: WelcomeAction, state: WelcomeState) {
        when (action) {
            is WelcomeAction.ProceedWithBiometrics -> proceedWithBiometrics(state)
            is WelcomeAction.ProceedWithCard -> proceedWithCard(state)
            is WelcomeAction.ClearUserWallets -> disableUserWalletsSaving()
            else -> Unit
        }
    }

    private fun disableUserWalletsSaving() = scope.launch {
        userWalletsListManager.clear()
            .flatMap { walletStoresManager.clear() }
            .flatMap { tangemSdkManager.clearSavedUserCodes() }
            .doOnFailure { e ->
                Timber.e(e, "Unable to clear user wallets")
            }
            .doOnResult {
                store.dispatchWithMain(WelcomeAction.CloseError)
                store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Home))
            }
    }

    private fun proceedWithBiometrics(state: WelcomeState) = scope.launch {
        userWalletsListManager.unlockIfLockable()
            .doOnFailure { error ->
                Timber.e(error, "Unable to unlock user wallets with biometrics")
                store.dispatchOnMain(WelcomeAction.ProceedWithBiometrics.Error(error))
            }
            .doOnSuccess { selectedUserWallet ->
                store.dispatchOnMain(SignInAction.SetSignInType(Basic.SignedIn.SignInType.Biometric))
                store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                store.dispatchOnMain(WelcomeAction.ProceedWithBiometrics.Success)
                store.onUserWalletSelected(userWallet = selectedUserWallet)

                state.intent?.let {
                    WalletConnectLinkIntentHandler().handleIntent(it)
                }
            }
    }

    private fun proceedWithCard(state: WelcomeState) = scope.launch {
        scanCardInternal { scanResponse ->
            val userWallet = UserWalletBuilder(scanResponse).build() ?: return@scanCardInternal

            userWalletsListManager.save(userWallet, canOverride = true)
                .doOnFailure { error ->
                    Timber.e(error, "Unable to save user wallet")
                    store.dispatchOnMain(WelcomeAction.ProceedWithCard.Error(error))
                }
                .doOnSuccess {
                    store.dispatchOnMain(SignInAction.SetSignInType(Basic.SignedIn.SignInType.Card))
                    store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                    store.dispatchOnMain(WelcomeAction.ProceedWithCard.Success)
                    store.onUserWalletSelected(userWallet = userWallet)

                    state.intent?.let {
                        WalletConnectLinkIntentHandler().handleIntent(it)
                    }
                }
        }
    }

    private fun handleInitialIntent(intent: Intent?) {
        val isBackgroundScanNotHandled = !intentHandler.handleBackgroundScan(intent, hasSavedUserWallets = true)
        val hasNotIncompletedBackup = !backupService.hasIncompletedBackup

        if (isBackgroundScanNotHandled && hasNotIncompletedBackup) {
            store.dispatchOnMain(WelcomeAction.ProceedWithBiometrics)
        }
    }

    private suspend inline fun scanCardInternal(crossinline onCardScanned: suspend (ScanResponse) -> Unit) {
        tangemSdkManager.setAccessCodeRequestPolicy(
            useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes,
        )
        ScanCardProcessor.scan(
            analyticsEvent = Basic.CardWasScanned(AnalyticsParam.ScannedFrom.SignIn),
            onSuccess = { scanResponse ->
                scope.launch { onCardScanned(scanResponse) }
            },
            onFailure = {
                when (it) {
                    is TangemSdkError.ExceptionError -> {
                        store.dispatchOnMain(WelcomeAction.ProceedWithCard.Success)
                    }
                    else -> {
                        store.dispatchOnMain(WelcomeAction.ProceedWithCard.Error(it))
                    }
                }
            },
            onWalletNotCreated = {
                store.dispatchOnMain(WelcomeAction.ProceedWithCard.Success)
            },
        )
    }
}
