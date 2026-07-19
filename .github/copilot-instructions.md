# Copilot Instructions

This repository is a Java 17 Maven chess project. Use the project files below as the source of truth for build and runtime flow:

- Build and test commands: `mvn compile`, `mvn test`, `mvn package`
- Focused regression commands: `mvn -Dtest=BoardParserTest test`, `mvn -Dtest=RuleEngineTest test`
- Main runtime entry point: [src/main/java/kfchess/Main.java](src/main/java/kfchess/Main.java)
- Maven configuration: [pom.xml](pom.xml)
- Layered architecture: [src/main/java/kfchess](src/main/java/kfchess)

Project-specific guidance:

- Keep the design flexible for future binary board and piece representation changes.
- Prefer configurable custom games and rule definitions over hard-coded behavior. When adding movement or game rules, look for existing extension points in [src/main/java/kfchess/rules/RuleEngine.java](src/main/java/kfchess/rules/RuleEngine.java) rather than introducing special-case logic.
- Keep the engine stateful per game instance instead of relying on global/shared state; follow the pattern in [src/main/java/kfchess/engine/GameEngine.java](src/main/java/kfchess/engine/GameEngine.java).
- Separate rules and UI concerns. Keep move legality/state transitions in the rules and engine layers, and avoid leaking board or game logic into the view layer.
- Avoid code smells by following DRY, SRP, and strong encapsulation principles.
- Keep constants and configuration values out of business logic whenever possible.
- Strive for high unit-test coverage for core behavior and edge cases, especially around text parsing and rule enforcement in [src/test/java/texttests](src/test/java/texttests).
- Avoid monkey patching in tests; prefer explicit, maintainable test setup.
- Add the repository URL in the main file header when updating the main entry point.

For startup context, read these files first in order:

1. [pom.xml](pom.xml)
2. [src/main/java/kfchess/Main.java](src/main/java/kfchess/Main.java)
3. [src/main/java/kfchess/engine/GameEngine.java](src/main/java/kfchess/engine/GameEngine.java)
4. [src/main/java/kfchess/rules/RuleEngine.java](src/main/java/kfchess/rules/RuleEngine.java)
5. [README.md](README.md)
