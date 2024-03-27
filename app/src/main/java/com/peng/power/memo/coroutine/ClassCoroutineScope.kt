package com.peng.power.memo.coroutine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class ClassCoroutineScope(private val dispatchers:CoroutineContext = Dispatchers.Default) :
    BaseCoroutineScope {
    override val job: Job = SupervisorJob()


    override fun releaseCorounitne() {
        job.cancel()
    }

    override val coroutineContext: CoroutineContext
        get() = dispatchers + job
}