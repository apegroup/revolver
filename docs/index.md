---
layout: default
title: Home
nav_order: 1
description: "Immutable event-based state management for Kotlin Multiplatform."
---

# Revolver Documentation

Revolver is an immutable, event-based state management framework for Kotlin Multiplatform. It enforces unidirectional data flow and a single source of truth for application state.

---

## Contents

| Document | Description |
|---|---|
| [Getting Started](getting-started.md) | Installation, minimum requirements, and your first ViewModel |
| [Core Concepts](concepts.md) | Event, State, Effect, ViewModel, and Emitter explained |
| [Error Handling](error-handling.md) | Built-in and custom error handlers, registration order |
| [Testing](testing.md) | Unit testing with Turbine and Mockative |
| [iOS Integration](ios-integration.md) | Swift observation patterns, DisposableHandle, lifecycle |
| [API Reference](api-reference.md) | Complete public API listing |

---

## How it works

```
Client ──emit(Event)──► RevolverViewModel ──► EventHandler
                                                   │
                             ┌─────────────────────┤
                             ▼                     ▼
                       StateFlow<State>    SharedFlow<Effect>
                             │                     │
                             └──────────► Client ◄─┘
```

| Type | Direction | Purpose |
|---|---|---|
| `RevolverEvent` | Client → ViewModel | User actions or lifecycle triggers |
| `RevolverState` | ViewModel → Client | Immutable snapshot of what to display |
| `RevolverEffect` | ViewModel → Client | One-shot side effect (navigation, toast, etc.) |

---

## Quick install

```kotlin
// settings.gradle.kts
maven {
    url = uri("https://maven.pkg.github.com/apegroup/revolver/")
    credentials {
        username = System.getenv("GH_USERNAME") ?: ""
        password = System.getenv("GH_TOKEN") ?: ""
    }
}

// build.gradle.kts — commonMain
implementation("com.umain:revolver:1.6.0")
```
