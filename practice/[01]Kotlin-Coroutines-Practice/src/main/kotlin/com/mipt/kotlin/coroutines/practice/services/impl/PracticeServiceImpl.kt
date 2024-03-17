package com.mipt.kotlin.coroutines.practice.services.impl

import com.mipt.kotlin.coroutines.practice.compute.model.Task
import com.mipt.kotlin.coroutines.practice.di.DependencyProvider
import com.mipt.kotlin.coroutines.practice.services.PracticeService
import kotlinx.coroutines.*

class PracticeServiceImpl: PracticeService {

    private val remoteTasksScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val computeTasksScope = CoroutineScope(Dispatchers.Default)

    private val remoteClient = DependencyProvider.provideRemoteClient()

    override suspend fun startWork() {
//        loadRemote()

        compute()
    }

    private suspend fun compute() {
        val computeTasks = DependencyProvider.generateTasksToCompute()
        val results = mutableListOf<Deferred<Int>>()

        for (genericTask in computeTasks) {
            val asyncResult = genericTask.computeAsync()
            results += asyncResult
        }

        results.awaitAll().forEach {
            println("Compute task result: $it")
        }
    }

    private fun Task.GenericComputeTask.computeAsync(): Deferred<Int> {
        return computeTasksScope.async {
            val deferredChildResults = mutableListOf<Deferred<Int>>()

            for (task in childTasks) {
                deferredChildResults += async {
                    task.calculate()
                }
            }

            val childResults = deferredChildResults.awaitAll()

           childResults.fold(0) { acc, result ->
               when(tasksOperator) {
                   Task.Operator.MINUS -> acc - result
                   Task.Operator.PLUS -> acc + result
               }
           }
        }
    }

    private fun Task.SingleComputeTask.calculate(): Int {
        return when(operator) {
            Task.Operator.MINUS -> leftOperand - rightOperand
            Task.Operator.PLUS -> leftOperand + rightOperand
        }
    }

    private suspend fun loadRemote() {
        val remoteIdentifiers = DependencyProvider.provideRemoteDataIdentifiers()
        val remoteJobs = mutableListOf<Job>()

        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            val targetCoroutineName = coroutineContext[CoroutineName.Key]

            println("Error in coroutine: ${targetCoroutineName?.name ?: "Unknown"}, $throwable")
        }

        for (identifier in remoteIdentifiers) {
            val coroutineName = CoroutineName("Coroutine($identifier)")
            val job = remoteTasksScope.launch(coroutineName + exceptionHandler) {
                println("Start loading data for id: $identifier")
                val result = remoteClient.loadRemotePayload(identifier)
                println("Loading data for id: $identifier finished, task result: ${result.payload.taskResult}")
            }

            remoteJobs += job
        }

        remoteJobs.joinAll()
    }
}