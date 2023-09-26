package com.tangem.feature.wallet.presentation.router

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.WalletFragment
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensScreen
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensViewModel
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletViewModel
import com.tangem.features.tokendetails.navigation.TokenDetailsArguments
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import kotlin.properties.Delegates

/** Default implementation of wallet feature router */
internal class DefaultWalletRouter(private val reduxNavController: ReduxNavController) : InnerWalletRouter {

    private var navController: NavHostController by Delegates.notNull()
    private var fragmentManager: FragmentManager by Delegates.notNull()

    override fun getEntryFragment(): Fragment = WalletFragment.create()

    @Composable
    override fun Initialize(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager

        NavHost(
            navController = rememberNavController().apply { navController = this },
            startDestination = WalletRoute.Wallet.route,
        ) {
            composable(WalletRoute.Wallet.route) {
                val viewModel = hiltViewModel<WalletViewModel>().apply { router = this@DefaultWalletRouter }
                LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

                WalletScreen(state = viewModel.uiState)
            }

            composable(
                WalletRoute.OrganizeTokens.route,
                arguments = listOf(navArgument(WalletRoute.userWalletIdKey) { type = NavType.StringType }),
            ) {
                val viewModel: OrganizeTokensViewModel = hiltViewModel<OrganizeTokensViewModel>()
                    .apply {
                        router = this@DefaultWalletRouter
                    }

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                OrganizeTokensScreen(
                    modifier = Modifier.statusBarsPadding(),
                    state = uiState,
                )
            }
        }
    }

    override fun popBackStack(screen: AppScreen?) {
        /*
         * It's hack that avoid issue with closing the wallet screen.
         * We are using NavGraph only inside feature so first backstack's element is entry of NavGraph and
         * next element is wallet screen entry.
         * If backstack contains only NavGraph entry and wallet screen entry then we close the wallet fragment.
         */
        if (navController.backQueue.size == BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN) {
            if (screen != null) {
                reduxNavController.navigate(action = NavigationAction.PopBackTo(screen))
            } else {
                fragmentManager.popBackStack()
            }
        } else {
            navController.popBackStack()
        }
    }

    override fun openOrganizeTokensScreen(userWalletId: UserWalletId) {
        navController.navigate(WalletRoute.OrganizeTokens.createRoute(userWalletId))
    }

    override fun openDetailsScreen() {
        // FIXME: Prepare details screen (e.g. dispatch action: `DetailsAction.PrepareScreen`)
        // https://tangem.atlassian.net/browse/AND-4259
        reduxNavController.navigate(action = NavigationAction.NavigateTo(AppScreen.Details))
    }

    override fun openOnboardingScreen() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
    }

    override fun openTxHistoryWebsite(url: String) {
        reduxNavController.navigate(action = NavigationAction.OpenUrl(url))
    }

    override fun openTokenDetails(currency: CryptoCurrency) {
        reduxNavController.navigate(
            action = NavigationAction.NavigateTo(
                screen = AppScreen.WalletDetails,
                bundle = bundleOf(
                    TokenDetailsRouter.TOKEN_DETAILS_ARGS to TokenDetailsArguments(
                        currencyId = currency.id,
                        currencyName = currency.name,
                        iconUrl = currency.iconUrl,
                        coinType = when (currency) {
                            is CryptoCurrency.Coin -> TokenDetailsArguments.CoinType.Native
                            is CryptoCurrency.Token -> TokenDetailsArguments.CoinType.Token(
                                standardName = currency.network.standardType.name,
                                networkName = currency.network.name,
                                networkIcon = currency.networkIconResId,
                            )
                        },
                    ),
                ),
            ),
        )
    }

    override fun openStoriesScreen() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(screen = AppScreen.Home))
    }

    override fun openSaveUserWalletScreen() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(AppScreen.SaveWallet))
    }

    override fun isWalletLastScreen(): Boolean = reduxNavController.getBackStack().lastOrNull() == AppScreen.Wallet

    override fun openManageTokensScreen() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(AppScreen.AddTokens))
    }

    private companion object {
        const val BACKSTACK_ENTRY_COUNT_TO_CLOSE_WALLET_SCREEN = 2
    }
}
