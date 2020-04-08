package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.tangem.CardManager
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.domain.actions.PersonalizeAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizeConfigConverter
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
 * Created by Anton Zhilenkov on 19/03/2020.
 */
class PersonalizeItemsManager(
        private val store: Store<PersonalizeConfig>
) : BaseItemsManager(PersonalizeAction()), LifecycleObserver {

    init {
        Log.d(this, "new instance created")
    }
    private val converter = PersonalizeConfigConverter()

    override fun createItemsList(): List<Item> {
        val config = store.restore()
        return converter.convert(config)
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(this, getAttrsForAction(cardManager), callback)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        val config = converter.convert(itemList, PersonalizeConfig())
        store.save(config)
    }
}

interface Store<M> {
    fun save(config: M)
    fun restore(): M
}