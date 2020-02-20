package com.piotrprus.testrxconcurrency

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <F, S, R> MediatorLiveData<R>.addSources(first: LiveData<F>, second: LiveData<S>, skipUpdating: Boolean = false, block: (F?, S?) -> R?) {
    addSource(first) { if (!skipUpdating) this.value = block.invoke(it, second.value) }
    addSource(second) { if (!skipUpdating) this.value = block.invoke(first.value, it) }
}