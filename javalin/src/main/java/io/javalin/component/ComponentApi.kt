package io.javalin.component

interface IdentifiableHook {
    val id: String
}

class ComponentNotFoundException(
    hook: IdentifiableHook
) : IllegalStateException("No component resolver registered for ${hook.id}")

class ComponentAlreadyExistsException(
    hook: IdentifiableHook
) : IllegalStateException("Component resolver already registered for ${hook.id}")
