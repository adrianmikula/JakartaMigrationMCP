# Simplicity & Consistency Guidelines

## Core Principles
- Remove duplication and unused files, methods or abstractions
- Make code 50% shorter/simpler if possible
- Don't over-complicate or over-engineer something simple
- Use pre-existing conventions/patterns
- Don't add fallback logic if it's not part of the requirements

## Code Organization
- One class per file unless closely related inner classes
- Max 500 lines per source file; split if exceeded
- Package-by-feature, not by layer
- Keep the public API surface minimal

## Naming
- Classes: PascalCase nouns
- Methods: camelCase verbs
- Constants: UPPER_SNAKE_CASE
- Avoid abbreviations except for well-known ones (e.g., config, util, info)

## Method Design
- Max 30 lines per method
- Single level of abstraction per method
- Early return over nested ifs
- Prefer immutability

## Error Handling
- Fail fast with clear messages
- Don't swallow exceptions silently
- Use specific exception types, not generic ones
- Log at the boundary, throw at the core
