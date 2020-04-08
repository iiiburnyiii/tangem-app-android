package com.tangem.tangemtest.ucase.domain.paramsManager

import com.tangem.CardManager
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence.ItemsChangeConsequence
import com.tangem.tasks.TaskEvent

typealias ActionResponse = TaskEvent<*>
typealias AffectedList = List<Item>
typealias AffectedItemsCallback = (AffectedList) -> Unit
typealias ActionCallback = (ActionResponse, AffectedList) -> Unit

/**
 * Created by Anton Zhilenkov on 19/03/2020.
 */
interface ItemsManager : PayloadHolder {

    fun itemChanged(id: Id, value: Any?, callback: AffectedItemsCallback? = null)
    fun setItems(items: List<Item>)
    fun getItems(): List<Item>
    fun setItemChangeConsequences(consequence: ItemsChangeConsequence?)
    fun invokeMainAction(cardManager: CardManager, callback: ActionCallback)
    fun getActionByTag(id: Id, cardManager: CardManager): ((ActionCallback) -> Unit)?
    fun attachPayload(payload: Payload)
}

interface PayloadKey {
    companion object {
        val cardConfig: String = "cardConfig"
        val incomingJson: String = "incomingJson"
        val card = "card"
        val actionView = "actionView"
        val itemList = "itemList"
    }
}