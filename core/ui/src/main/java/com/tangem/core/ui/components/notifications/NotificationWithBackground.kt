package com.tangem.core.ui.components.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Custom notification with image background
 * @see [Swap Promo](https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=8713-6307&mode=dev)
 */
@Suppress("LongMethod", "DestructuringDeclarationWithTooManyEntries")
@Composable
fun NotificationWithBackground(config: NotificationConfig, modifier: Modifier = Modifier) {
    val button = config.buttonsState as? NotificationConfig.ButtonsState.SecondaryButtonConfig

    ConstraintLayout(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size62)
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium),
    ) {
        val (iconRef, titleRef, subtitleRef, closeIconRef, buttonRef, backgroundRef) = createRefs()

        val spacing2 = TangemTheme.dimens.spacing2
        val spacing12 = TangemTheme.dimens.spacing12
        val spacing14 = TangemTheme.dimens.spacing14

        Image(
            painter = painterResource(config.backgroundResId ?: R.drawable.img_swap_promo_banner_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.constrainAs(backgroundRef) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                visibility = if (config.backgroundResId == null) Visibility.Gone else Visibility.Visible
            },
        )
        Image(
            painter = painterResource(config.iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(TangemTheme.dimens.size34)
                .constrainAs(iconRef) {
                    start.linkTo(parent.start, spacing12)
                    linkTo(titleRef.top, subtitleRef.bottom, bias = 0.1f)
                },
        )
        Text(
            text = config.title.resolveReference(),
            style = TangemTheme.typography.button,
            color = TangemTheme.colors.text.constantWhite,
            modifier = Modifier.constrainAs(titleRef) {
                top.linkTo(parent.top, spacing12)
                start.linkTo(iconRef.end, spacing12)
                end.linkTo(closeIconRef.start, spacing2)
                width = Dimension.fillToConstraints
            },
        )
        Text(
            text = config.subtitle.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.constantWhite,
            modifier = Modifier.constrainAs(subtitleRef) {
                top.linkTo(titleRef.bottom, spacing2)
                start.linkTo(iconRef.end, spacing12)
                end.linkTo(parent.end, spacing12)
                bottom.linkTo(buttonRef.top, spacing12, spacing14)
                width = Dimension.fillToConstraints
            },
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_close_24),
            contentDescription = null,
            tint = TangemTheme.colors.text.constantWhite,
            modifier = Modifier
                .size(TangemTheme.dimens.size16)
                .constrainAs(closeIconRef) {
                    top.linkTo(parent.top, spacing12)
                    end.linkTo(parent.end, spacing12)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                ) {
                    config.onCloseClick?.invoke()
                },
        )
        SecondaryButtonIconStart(
            text = button?.text?.resolveReference().orEmpty(),
            iconResId = button?.iconResId ?: R.drawable.ic_exchange_vertical_24,
            onClick = button?.onClick ?: {},
            modifier = Modifier.constrainAs(buttonRef) {
                start.linkTo(parent.start, spacing12)
                end.linkTo(parent.end, spacing12)
                bottom.linkTo(parent.bottom, spacing12)
                width = Dimension.fillToConstraints
                visibility = if (button == null) {
                    Visibility.Gone
                } else {
                    Visibility.Visible
                }
            },
        )
    }
}

//region preview
@Preview
@Composable
private fun NotificationWithBackgroundPreview(
    @PreviewParameter(NotificationWithBackgroundPreviewProvider::class) config: NotificationConfig,
) {
    TangemTheme {
        NotificationWithBackground(config = config)
    }
}

private class NotificationWithBackgroundPreviewProvider : PreviewParameterProvider<NotificationConfig> {
    override val values: Sequence<NotificationConfig>
        get() = sequenceOf(
            NotificationConfig(
                title = resourceReference(id = R.string.main_swap_promotion_title),
                subtitle = resourceReference(id = R.string.main_swap_promotion_message),
                iconResId = R.drawable.img_swap_promo,
                backgroundResId = R.drawable.img_swap_promo_banner_background,
            ),
            NotificationConfig(
                title = resourceReference(id = R.string.token_swap_promotion_title),
                subtitle = stringReference(
                    "Swap multiple currencies between any chains you wish. Swap multiple " +
                        "currencies between any chains you wish. Swap multiple " +
                        "currencies between any chains you wish. Commission free period " +
                        "till Dec 31.",
                ),
                iconResId = R.drawable.img_swap_promo,
                backgroundResId = R.drawable.img_swap_promo_banner_background,
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(id = R.string.token_swap_promotion_button),
                    onClick = {},
                ),
            ),
        )
}
//endregion
