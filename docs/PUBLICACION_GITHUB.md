# Publicar MoodWave en GitHub

## 1. Revisar la copia preparada

Este proyecto ya excluye compilaciones, cachés de Android Studio, configuraciones locales y archivos de claves. Verifica de nuevo que no contenga información personal antes de publicarlo.

## 2. Crear repositorio remoto

En GitHub selecciona **New repository**. Nombre sugerido: `MoodWave`. Puedes seleccionar visibilidad **Private** al comienzo y cambiarla después de la revisión. **No inicialices con README ni .gitignore**, porque esos archivos ya están en esta carpeta.

## 3. Primer commit

Desde Terminal en la carpeta del proyecto:

```bash
git init
git branch -M main
git add .
git status
git commit -m "Initial commit: MoodWave Android app and documentation"
```

Antes de hacer commit, verifica que `git status` no liste `local.properties`, `build`, `.gradle`, `.idea`, claves ni bases con datos reales.

## 4. Conectar con GitHub

Reemplaza el nombre de usuario y el nombre del repositorio por los reales:

```bash
git remote add origin https://github.com/USUARIO/MoodWave.git
git push -u origin main
```

GitHub requiere autenticación apropiada (por ejemplo, mediante su navegador o GitHub CLI) para publicar los cambios.

## 5. Acreditar a los dos integrantes

Invita a la cuenta de Joel o Juan, según quién sea propietario, desde **Settings → Collaborators**. Los dos nombres ya aparecen en README y en `docs/COLABORACION.md`. Cada persona debería realizar sus propios commits para que GitHub pueda mostrar contribuciones genuinas.

## 6. Completar presentación pública

- Añade la descripción del repositorio: `Diario emocional Android con estadísticas, K-means y Health Connect | IPN UPIIT`.
- Agrega etiquetas como `android`, `kotlin`, `jetpack-compose`, `room`, `health-connect`, `k-means`, `academic-project`.
- Incorpora capturas **sin datos personales** en `docs/capturas/` y enlázalas desde `README.md`.
- Reemplaza `URL_DEL_REPOSITORIO` en el README por la dirección definitiva.
- Si ambos autores quieren que terceros reutilicen el código, decidan una licencia y añádanla explícitamente.
