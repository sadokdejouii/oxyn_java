# AGENTS

## Project shape (what matters first)
- Single-module Maven JavaFX desktop app (not a monorepo); main entrypoint is `org.example.mains.MainFX` (`src/main/java/org/example/mains/MainFX.java`).
- UI is FXML/CSS-driven: controllers in `src/main/java/org/example/controllers`, views in `src/main/resources/FXML`, styles in `src/main/resources/css`.
- `target/classes/**` mirrors compiled resources; make edits under `src/main/resources/**`, not `target/**`.

## Canonical commands
- Run app: `mvn javafx:run`
- Run tests: `mvn test`
- Run one test class: `mvn -Dtest=EvenementServicesTest test`
- Run one test method: `mvn -Dtest=CommandeReglesTest#ageEnJoursAuMoins test`
- Debug JavaFX (suspend on 5005): `mvn -Pjavafx-debug javafx:run`

## Environment and runtime gotchas
- Java target is 17 in `pom.xml`; JavaFX plugin also passes `--enable-native-access=javafx.graphics`.
- Default DB connection is hardcoded in `MyDataBase` to MySQL `jdbc:mysql://localhost:3306/oxyn`, user `root`, empty password (`src/main/java/org/example/utils/MyDataBase.java`).
- Many service tests are integration-style and hit the real DB (they insert/update/delete data), especially under `src/test/java/org/example/services/*`.
- Safer quick verification without DB side effects: run focused pure-unit tests under `src/test/java/org/example/utils/*` first.

## Feature-specific config sources
- SendGrid config resolution order: env (`SENDGRID_*`) -> JVM props (`-Dsendgrid.*`) -> `~/.oxyn/sendgrid.properties` (`src/main/java/org/example/notifications/SendGridConfig.java`).
- Temporal trust and digital will features also read optional overrides from `~/.oxyn/*.properties` and env/system props (`src/main/java/org/example/temporal/TemporalTrustConfig.java`, `src/main/java/org/example/digitalwill/DigitalWillConfig.java`).
- Windows Hello depends on PowerShell script resource `src/main/resources/powershell/windows-hello-bridge.ps1` and is Windows-specific.
- Product image uploads are stored in runtime working dir `product_images/` (`System.getProperty("user.dir")`), so launching from different directories changes where files are written.

## High-signal workflow notes
- If UI changes look ignored, check you edited `src/main/resources/**` and rerun `mvn javafx:run` (not stale `target/classes` copies).
- When touching services/controllers that use DB, prefer running targeted tests before full `mvn test` to avoid long, data-mutating runs.
