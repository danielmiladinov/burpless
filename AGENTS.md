# AGENTS.md

This file contains instructions for agentic coding assistants working on the burpless project.
Burpless is a Clojure wrapper around Cucumber-JVM for writing BDD feature tests.

## Project Overview

Burpless provides an idiomatic Clojure interface for Cucumber-JVM, enabling developers to write BDD tests using Gherkin feature files.
The library supports:

- Step definitions with state management
- DataTables and DocStrings
- Custom parameter types
- Hooks (before/after scenarios, steps; around steps)
- Generative testing capabilities

## Build Commands

### Full Test Suite
```bash
clojure -T:build test
```

### Clean Build
```bash
clojure -T:build clean
```

### CI Pipeline (tests + JAR build)
```bash
clojure -T:build ci
```

### Install JAR Locally
```bash
clojure -T:build install
```

### Deploy to Clojars
```bash
clojure -T:build deploy
```

## Test Commands

### Run All Tests
```bash
clojure -T:build test
```

### Run Single Test File
```bash
clojure -M:test -m cognitect.test-runner -r "test-file-pattern"
```

### Run Tests with Specific Namespace
```bash
clojure -M:test -m cognitect.test-runner -n "namespace.name"
```

### Run Specific Test Function
```bash
clojure -M:test -m cognitect.test-runner -v "test-function-name"
```

## Lint Commands

### Run clj-kondo Linting
```bash
clj-kondo --lint src test
```

### Run clj-kondo with Config
```bash
clj-kondo --config .clj-kondo/config.edn --lint src test
```

## Code Style Guidelines

### Naming Conventions

- **Namespaces**: Use kebab-case, e.g., `burpless.runtime`, `burpless.generative`
- **Functions**: Use kebab-case for multi-word names, e.g., `run-cucumber`, `create-cucumber-runtime`
- **Variables**: Use kebab-case, e.g., `state-atom`, `feature-path`
- **Macros**: Follow function naming conventions, e.g., `step`, `hook`, `parameter-type`
- **Constants**: Use UPPER_SNAKE_CASE only when necessary (rare in Clojure)
- **Test files**: Mirror source namespace structure, e.g., `src/burpless/core.clj` → `test/burpless/core_test.clj`

### Imports and Requires

- Use `:require` with aliases for external namespaces:
```clojure
(:require [clojure.test :refer [deftest is testing]]
          [burpless :refer [run-cucumber step]])
```

- Group requires logically: standard library, third-party, project internal
- Use `:import` for Java classes:
```clojure
(:import (io.cucumber.datatable DataTable)
         (java.lang String Integer))
```

- Prefer `:refer` over `:use` for selective imports
- Use namespace-qualified keywords for domain-specific data

### Formatting and Structure

- **Indentation**: Use 2 spaces (standard Clojure)
- **Line length**: Aim for 100 characters or less
- **Blank lines**: Use to separate logical sections
- **Comments**: Use `;;` for line comments, place above the code they describe
- **Docstrings**: Required for public functions, follow Clojure conventions

### Function and Macro Definitions

- Public functions/macros should have docstrings
- Parameter destructuring should be clear and documented
- Use type hints for Java interop when beneficial:
```clojure
(fn [state ^DataTable data-table]
  ...)
```

### Error Handling

- Use `clojure.test/is` directly in step functions (throws AssertionError for Cucumber-JVM compatibility)
- Use `clojure.test/is` for assertions in test setup/teardown code
- Control failure reporting with `burpless.runtime/*report-all-step-failures*` (defaults to single failure)
- Prefer explicit error checking over exceptions where possible
- Use meaningful error messages that help with debugging

### State Management

- Use atoms for mutable state in step functions:
```clojure
(def state-atom (atom {}))
```

- Follow functional programming principles where possible
- State transformations should be pure functions

### Testing Patterns

- Use `deftest` for test definitions
- Use `clojure.test/is` for assertions in step functions (provides detailed error reporting and Cucumber-JVM compatibility)
- Use `clojure.test/is` for assertions in test setup/teardown code
- Test names should be descriptive: `deftest feature-name-test`
- Group related tests in namespaces by functionality

### Cucumber-Specific Conventions

- **Step definitions**: Use descriptive regex/Cucumber expressions
- **State passing**: First parameter is always current state, return updated state
- **Hook ordering**: Use `:order` parameter for hook execution sequence
- **Tag expressions**: Use in hooks for conditional execution

### File Organization

- `src/`: Main source code
- `test/`: Test files and feature files
- `test-resources/features/`: Gherkin feature files
- `resources/`: Static resources
- Keep related functionality in the same namespace
- Split large namespaces when they exceed ~300 lines

### Performance Considerations

- Use type hints for Java interop to avoid reflection overhead
- Be mindful of atom usage in hot code paths
- Profile when optimizing

### Security

- Avoid logging sensitive information
- Use secure random generation when needed
- Follow principle of least privilege

### Documentation

- Keep README.md up to date with usage examples
- Document breaking changes in CHANGELOG.md
- Use semantic versioning (currently at 1.0.0)

## Additional Tools

### REPL Development
```bash
clojure -M:test:nrepl  # Start nREPL for development
```

### Dependency Analysis
```bash
clojure -M:deps:tree   # Show dependency tree
```

## Commit Guidelines

- NEVER COMMIT ANYTHING ON THE USER'S BEHALF

## Pre-commit Hooks

The project uses clj-kondo for linting. Ensure code passes linting before committing.

## User Interaction Preferences

- Be responsive to user feedback and concerns about technical feasibility
- Acknowledge when solutions are incomplete or don't meet requirements
- Prefer honest assessment of difficulties over optimistic but inadequate implementations
- Do not push forward with solutions that don't fully address the stated requirements
- When technical challenges are identified, discuss them openly rather than attempting incomplete workarounds
