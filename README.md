# proyecto

Aplicación **Android (Kotlin)**.

## Estructura (vista parcial)

```text
proyecto/
  .cursor/
    rules/
  .git/
    COMMIT_EDITMSG
    HEAD
    config
    description
    hooks/
    index
    info/
    logs/
    objects/
    refs/
  .gitignore
  .gradle/
    8.13/
    buildOutputCleanup/
    config.properties
    file-system.probe
    vcs-1/
  .idea/
    .gitignore
    AndroidProjectSystem.xml
    caches/
    compiler.xml
    deploymentTargetSelector.xml
    deviceManager.xml
    gradle.xml
    inspectionProfiles/
    migrations.xml
    misc.xml
    runConfigurations.xml
    vcs.xml
    workspace.xml
  __pycache__/
    server.cpython-311.pyc
  app/
    .gitignore
    build/
    build.gradle.kts
    proguard-rules.pro
    sampledata/
    src/
  build/
    kotlin/
    reports/
  build.gradle.kts
  gradle/
    libs.versions.toml
    wrapper/
  gradle.properties
  gradlew
  gradlew.bat
  local.properties
  requirements.txt
  server.py
  settings.gradle.kts
```

## Requisitos

- Git
- Android Studio Flamingo+ (JDK 17)
- Gradle (incluido en wrapper)

## App Android (Kotlin)

1. Abre la carpeta del proyecto en **Android Studio**.

2. Configura la base URL de tu API (emulador usa `10.0.2.2`).

```kotlin
// ApiClient.kt (ejemplo)
private const val BASE_URL = "http://10.0.2.2:8000/"
```

## Uso

Describe aquí cómo ejecutar y probar las funcionalidades principales.

## Configuración & Variables de Entorno

- Crea un archivo `.env` (si aplica) para credenciales y claves.
- No subas claves ni secretos al repositorio.

## Troubleshooting

- **No conecta desde emulador Android**: usa `http://10.0.2.2:8000/` en lugar de `http://127.0.0.1:8000/`.
- **401 Unauthorized**: revisa credenciales y método HTTP (`POST`) y `Content-Type: application/json`.
- **Error de dependencias**: ejecuta `pip install -r requirements.txt` o sincroniza Gradle.

## Comandos útiles de Git

```bash
git init
git add .
git commit -m "chore: primer commit"
git branch -M main
git remote add origin https://github.com/USUARIO/REPO.git
git push -u origin main
```

## Licencia

MIT

## Backend (Python sencillo)
Proyecto detectó un `server.py`. Si es **Flask/FastAPI** u otro microframework, prueba:
```bash
pip install -r requirements.txt
python server.py
```
- Si usa **Uvicorn/FastAPI**: `uvicorn server:app --reload --host 0.0.0.0 --port 8000`
- Ajusta la **BASE_URL** en Android según el puerto.
