package com.peng.power.memo.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface BaseCoroutineScope : CoroutineScope {
    val job:Job

    //For coroutine job cancel
    fun releaseCorounitne()

}