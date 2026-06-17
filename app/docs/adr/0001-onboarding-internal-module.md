# ADR-0001: Onboarding as in-app internal module

## Status

Accepted

## Context

Onboarding is an in-app feature module. OpenCore uses a single `:app` Gradle module; feature boundaries are enforced by package structure and Kotlin `internal` visibility, not separate Gradle libraries.

## Decision

1. Place onboarding under `io.github.bengidev.opencore.onboarding/` with layered sub-packages.
2. Default types to `internal`; expose only `OnboardingScreen` and `OnboardingFacade` to the app shell.
3. Apply GoF patterns: Command (`OnboardingIntent`), Strategy (`PageDemoDefaultsStrategy`), Factory Method, Repository, Facade.
4. Use OpenCore branding in copy and theme tokens (`OpenCorePalette`, `OpenCoreOnboardingTheme`).
5. TDD: unit tests under `app/src/test/.../onboarding/`.

## Consequences

- No cross-module Gradle coordination; compiler enforces boundaries via `internal`.
- Promoting to a Gradle library later remains possible if boundaries need stronger isolation.
