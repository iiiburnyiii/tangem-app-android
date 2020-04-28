package com.tangem.devkit.ucase.variants.depersonalize.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.DepersonalizeItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

/**
 * Created by Anton Zhilenkov on 10.03.2020.
 */
class DepersonalizeActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { DepersonalizeItemsManager() }
}