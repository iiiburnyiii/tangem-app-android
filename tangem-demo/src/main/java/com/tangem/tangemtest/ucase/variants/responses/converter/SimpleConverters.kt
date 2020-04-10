package com.tangem.tangemtest.ucase.variants.responses.converter

import com.tangem.commands.Card
import com.tangem.commands.SignResponse
import com.tangem.commands.personalization.DepersonalizeResponse
import com.tangem.common.CompletionResult
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ModelToItems
import com.tangem.tangemtest._arch.structure.impl.TextItem
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
 * Created by Anton Zhilenkov on 07/04/2020.
 */
class ReadEventConverter : ModelToItems<CompletionResult.Success<Card>> {
    override fun convert(from: CompletionResult.Success<Card>): List<Item> = CardConverter().convert(from.data)
}

class SignResponseConverter : ModelToItems<SignResponse> {

    override fun convert(from: SignResponse): List<Item> {
        return listOf(
                TextItem(StringId("CID"), from.cardId),
                TextItem(StringId("Wallet signed hashes"), stringOf(from.walletSignedHashes)),
                TextItem(StringId("Wallet remaining signatures"), stringOf(from.walletRemainingSignatures)),
                TextItem(StringId("Signature"), stringOf(from.signature))
        )
    }
}

class DepersonalizeResponseConverter : ModelToItems<DepersonalizeResponse> {
    override fun convert(from: DepersonalizeResponse): List<Item> {
        return listOf(TextItem(StringId("Is success"), stringOf(from.success)))
    }
}