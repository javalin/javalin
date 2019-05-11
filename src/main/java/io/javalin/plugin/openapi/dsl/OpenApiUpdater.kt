package io.javalin.plugin.openapi.dsl

@FunctionalInterface
interface OpenApiUpdater<T> {
    fun applyUpdates(value: T)
}

typealias ApplyUpdates<T> = (value: T) -> Unit

fun <T> createUpdaterIfNotNull(function: ApplyUpdates<T>?) = function?.let { createUpdater(it) }

fun <T> createUpdater(function: ApplyUpdates<T>) = object : OpenApiUpdater<T> {
    override fun applyUpdates(value: T) = function(value)
}

fun <T> List<OpenApiUpdater<T>>.applyAllUpdates(value: T) = this.forEach { it.applyUpdates(value) }
