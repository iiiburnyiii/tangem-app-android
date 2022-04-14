package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.domain.ErrorConverter
import com.tangem.domain.common.form.DataField
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.features.addCustomToken.AddCustomTokenError
import com.tangem.domain.features.addCustomToken.AddCustomTokenWarning
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.features.addCustomToken.redux.ScreenState
import com.tangem.domain.features.addCustomToken.redux.ViewStates
import com.tangem.domain.redux.domainStore
import com.tangem.tap.common.compose.ComposeDialogManager
import com.tangem.tap.common.compose.keyboardAsState
import com.tangem.tap.features.tokens.addCustomToken.DomainErrorConverter
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 23/03/2022.
 */
private class AddCustomTokenScreen {} // for simple search

@Composable
fun AddCustomTokenScreen(state: MutableState<AddCustomTokenState>) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = colorResource(id = R.color.backgroundLightGray),
        floatingActionButton = {
            HangingOverKeyboardView(keyboardState = keyboardAsState()) {
                AddButton(state)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
//                item { AddCustomTokenDebugActions() }
                item {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = MaterialTheme.shapes.small,
                        elevation = 4.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            FormFields(state)
                        }
                    }
                }
                item { Warnings(state.value.warnings.toList()) }
            }
        }
        ComposeDialogManager()
    }

    LaunchedEffect(key1 = Unit, block = { domainStore.dispatch(AddCustomTokenAction.OnCreate) })
    DisposableEffect(key1 = Unit, effect = { onDispose { domainStore.dispatch(AddCustomTokenAction.OnDestroy) } })
}

@Composable
private fun FormFields(state: MutableState<AddCustomTokenState>) {
    val context = LocalContext.current
    val errorConverter = remember { DomainErrorConverter(context) }

    val stateValue = state.value
    stateValue.form.fieldList.forEach { field ->
        val data = ScreenFieldData.fromState(field, stateValue, errorConverter)
        when (field.id) {
            ContractAddress -> TokenContractAddressView(data)
            Network -> TokenNetworkView(data, stateValue)
            Name -> TokenNameView(data)
            Symbol -> TokenSymbolView(data)
            Decimals -> TokenDecimalsView(data)
            DerivationPath -> TokenDerivationPathView(data, stateValue)
        }
    }
}

@Composable
fun Warnings(warnings: List<AddCustomTokenWarning>) {
    if (warnings.isEmpty()) return

    val context = LocalContext.current
    val warningConverter = remember { DomainErrorConverter(context) }

    Column {
        warnings.forEachIndexed { index, item ->
            val modifier = when (index) {
                0 -> Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp)
                warnings.lastIndex -> Modifier.padding(16.dp, 8.dp, 16.dp, 16.dp)
                else -> Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp)
            }
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = colorResource(id = R.color.warning_warning),
                elevation = 4.dp,
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = warningConverter.convertError(item),
                    color = colorResource(id = R.color.lightGray0),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AddButton(state: MutableState<AddCustomTokenState>) {
    AddCustomTokenFab(
        modifier = Modifier
            .widthIn(210.dp, 280.dp),
        isEnabled = state.value.screenState.addButton.isEnabled
    ) { domainStore.dispatch(AddCustomTokenAction.OnAddCustomTokenClicked) }
}

@Composable
fun AddCustomTokenFab(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = if (isEnabled) {
        Color.White
    } else {
        colorResource(id = R.color.darkGray1)
    }
    val backgroundColor = Color(0xFF1ACE80)

    ExtendedFloatingActionButton(
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Filled.Add,
                tint = contentColor,
                contentDescription = "Add",
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.common_add),
            )
        },
        onClick = onClick,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
    )
}

data class ScreenFieldData(
    val field: DataField<*>,
    val error: AddCustomTokenError?,
    val errorConverter: ErrorConverter<String>,
    val viewState: ViewStates.TokenField
) {
    companion object {
        fun fromState(
            field: DataField<*>,
            state: AddCustomTokenState,
            errorConverter: DomainErrorConverter
        ): ScreenFieldData {
            return ScreenFieldData(
                field = field,
                error = state.getError(field.id),
                errorConverter = errorConverter,
                viewState = selectField(field.id, state.screenState)
            )
        }

        private fun selectField(id: FieldId, screenState: ScreenState): ViewStates.TokenField {
            return when (id) {
                ContractAddress -> screenState.contractAddressField
                Network -> screenState.network
                Name -> screenState.name
                Symbol -> screenState.symbol
                Decimals -> screenState.decimals
                DerivationPath -> screenState.derivationPath
                else -> throw UnsupportedOperationException()
            }
        }
    }
}