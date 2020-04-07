package com.tangem.tangemtest._main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangem.tasks.TaskEvent
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
 * Created by Anton Zhilenkov on 24/03/2020.
 */
class MainViewModel : ViewModel() {
    val ldDescriptionSwitch = MutableLiveData<Boolean>(false)
    val ldResponseEvent = MutableLiveData<TaskEvent<*>>()

    fun switchToggled(state: Boolean) {
        ldDescriptionSwitch.postValue(state)
    }

    fun changeResponseEvent(event: TaskEvent<*>?) {
        Log.d(this, "changeResponseEvent")
        val reqEvent = event ?: return

        ldResponseEvent.value = reqEvent
    }
}