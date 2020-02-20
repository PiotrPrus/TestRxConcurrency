package com.piotrprus.testrxconcurrency

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class MainViewModel : ViewModel() {

    val tasksInProgress = MediatorLiveData<Boolean>()
    private var customExecutor = Executors.newFixedThreadPool(1)
    private val disposablesA = CompositeDisposable()
    private val disposablesB = CompositeDisposable()
    private var counterA = AtomicInteger()
    private var counterB = AtomicInteger()
    private val _taskAinProgress = MutableLiveData<Boolean>(false)
    private val taskAinProgress: LiveData<Boolean>
        get() = _taskAinProgress

    private fun setTaskAProgress(isRunning: Boolean) {
        _taskAinProgress.postValue(isRunning)
    }
    private val _taskBinProgress = MutableLiveData<Boolean>(false)
    private val taskBinProgress: LiveData<Boolean>
        get() = _taskBinProgress

    private fun setTaskBProgress(isRunning: Boolean) {
        _taskBinProgress.postValue(isRunning)
    }

    init {
        tasksInProgress.addSources(taskAinProgress, taskBinProgress) { taskA, taskB ->
            taskA == true || taskB == true
        }
    }


    private fun executeA(variable: Int) {
        var startTimestamp = 0L
        randomListSortedAndAggregated(variable.times(10000))
            .subscribeOn(Schedulers.from(customExecutor))
            .observeOn(Schedulers.from(customExecutor))
            .doOnSubscribe {
                counterA.incrementAndGet()
                setTaskAProgress(true)
                startTimestamp = System.currentTimeMillis()
                Log.d("Task_A", "Started($variable) at $startTimestamp")
            }
            .doFinally {
                Log.d("Task_A", "doFinally")
                decreaseAndCheckCounterA()
            }
            .doOnDispose {
                val processDuration = System.currentTimeMillis().minus(startTimestamp)
                Log.d("Task_A", "Cancelled($variable) after $processDuration")
                decreaseAndCheckCounterA()
            }
            .subscribe({
                val processDuration = System.currentTimeMillis().minus(startTimestamp)
                Log.d("Task_A", "Finished($variable) in time: $processDuration with result: $it")
            }, {
                Log.d("Task_A", "Error($variable), Caught exception during sorting: $it")
            })
            .addToComposite(disposablesA)
    }

    private fun decreaseAndCheckCounterA() {
        val currentCounterA = counterA.decrementAndGet()
        if (currentCounterA == 0) {
            setTaskAProgress(false)
        }
    }

    private fun decreaseAndCheckCounterB() {
        val currentCounterB = counterB.decrementAndGet()
        if (currentCounterB == 0) {
            setTaskBProgress(false)
        }
    }

    private fun executeB(variable: Int) {
        var startTimestamp = 0L
        randomListBubbleSortLastItem(variable.times(1000))
            .subscribeOn(Schedulers.from(customExecutor))
            .observeOn(Schedulers.from(customExecutor))
            .doOnSubscribe {
                counterB.incrementAndGet()
                setTaskBProgress(true)
                startTimestamp = System.currentTimeMillis()
                Log.d("Task_B", "Started($variable) at $startTimestamp")
            }
            .doOnDispose {
                val processDuration = System.currentTimeMillis().minus(startTimestamp)
                Log.d("Task_B", "Cancelled($variable) after $processDuration")
                decreaseAndCheckCounterB()
            }
            .doFinally {
                Log.d("Task_B", "doFinally")
                decreaseAndCheckCounterB()
            }
            .subscribe({
                val processDuration = System.currentTimeMillis().minus(startTimestamp)
                Log.d("Task_B", "Finished($variable) in time: $processDuration with result: $it")
            }, {
                Log.d("Task_B", "Error($variable), Caught exception during bubble sort: $it")

            })
            .addToComposite(disposablesB)
    }

    private fun randomListSortedAndAggregated(variable: Int): Single<Long> =
        Single.create<Long> { emitter ->
            val randomList = List(variable) { Random.nextInt(1, 10) }
            val fibonacciList = randomList.map { value ->
                val seq = generateSequence(
                    Pair(0L, 1L),
                    { Pair(it.second, it.first + it.second) }).map { it.second }
                seq.take(value).last()
            }
            val sorted = fibonacciList.sorted()
            emitter.onSuccess(sorted.last())
        }

    private fun randomListBubbleSortLastItem(variable: Int): Single<Int> =
        Single.create<Int> { emitter ->
            val arr = List(variable) { Random.nextInt(1, variable) }.toIntArray()
            val n = arr.size
            for (i in 0 until n - 1) for (j in 0 until n - i - 1) if (arr[j] > arr[j + 1]) {
                val temp = arr[j]
                arr[j] = arr[j + 1]
                arr[j + 1] = temp
            }
            emitter.onSuccess(arr.last())
        }

    fun onAButtonClicked(variableA: String) {
        variableA.toIntOrNull()?.let { executeA(it) }
    }

    fun onBButtonClicked(variableB: String) {
        variableB.toIntOrNull()?.let { executeB(it) }
    }

    fun setThreadPoolClicked(seekBarValue: Int) {
        val threads = if (seekBarValue == 0) 1 else seekBarValue
        customExecutor = Executors.newFixedThreadPool(threads)
    }

    fun onCancelAClicked() {
        disposablesA.clear()
    }

    fun onCancelBClicked() {
        disposablesB.clear()
    }

    private fun Disposable.addToComposite(composite: CompositeDisposable) {
        composite.add(this)
    }

    override fun onCleared() {
        disposablesA.clear()
        disposablesB.clear()
    }
}

