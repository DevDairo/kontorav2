# Crear un repositorio Git nuevo

## Estado detectado

La carpeta `.git` actual existe, pero esta vacia: no contiene `HEAD`, `config`
ni objetos. Por eso `git status` informa que el proyecto no es un repositorio.
Aunque no hay historial que conservar, el procedimiento incluye validaciones
para no eliminar otra ruta por error.

No ejecutar esta limpieza hasta terminar la demostracion, respaldar el proyecto
y confirmar que no se necesita recuperar el historial anterior.

## 1. Preparar un remoto vacio

En GitHub, GitLab o el proveedor elegido:

1. crear un repositorio privado nuevo;
2. no generar README, `.gitignore` ni licencia;
3. copiar su URL HTTPS o SSH;
4. no subir archivos manualmente desde el navegador.

## 2. Comprobar la raiz

Desde PowerShell, dentro de `C:\Users\corre\Documentos\kontora`:

```powershell
$projectRoot = (Resolve-Path .).Path
$gitDirectory = Join-Path $projectRoot '.git'

if ((Split-Path $projectRoot -Leaf) -ne 'kontora') {
    throw "Ruta inesperada: $projectRoot"
}

[PSCustomObject]@{
    Proyecto = $projectRoot
    GitExiste = Test-Path -LiteralPath $gitDirectory
    GitTieneHead = Test-Path -LiteralPath (Join-Path $gitDirectory 'HEAD')
    GitTieneConfig = Test-Path -LiteralPath (Join-Path $gitDirectory 'config')
} | Format-List
```

En el estado actual, `GitExiste=True`, pero `GitTieneHead=False` y
`GitTieneConfig=False`.

## 3. Verificar archivos locales sensibles

Estos archivos pueden existir en el equipo, pero deben permanecer fuera del
nuevo repositorio:

- `infra/.env`;
- `frontend/.env`;
- respaldos `*.dump`, `*.backup` y `backups/`;
- `.codex/` y `.agents/`;
- `target/`, `node_modules/` y `dist/`.

No borrar `infra/.env`: contiene la configuracion de la demostracion. La
proteccion se comprobara despues de inicializar Git.

## 4. Eliminar solamente el metadato Git anterior

La siguiente accion es destructiva para el historial Git, no para el codigo.
El guard impide ejecutarla fuera de una carpeta llamada `kontora`:

```powershell
$projectRoot = (Resolve-Path .).Path
$gitDirectory = Join-Path $projectRoot '.git'

if ((Split-Path $projectRoot -Leaf) -ne 'kontora') {
    throw "No se eliminara .git fuera de la carpeta kontora"
}

if (Test-Path -LiteralPath $gitDirectory) {
    Remove-Item -LiteralPath $gitDirectory -Recurse -Force
}

if (Test-Path -LiteralPath $gitDirectory) {
    throw "No se pudo retirar el metadato Git anterior"
}
```

No ejecutar `Remove-Item` sobre la raiz del proyecto, `Documentos`, el perfil
del usuario o una variable sin validar.

## 5. Inicializar el repositorio

```powershell
git init -b main
git status
```

Comprobar que los secretos y artefactos estan ignorados:

```powershell
$pathsThatMustBeIgnored = @(
    'infra/.env',
    'backups',
    '.codex',
    '.agents'
)

foreach ($path in $pathsThatMustBeIgnored) {
    git check-ignore -v -- $path
}
```

Si `infra/.env` no aparece como ignorado, detenerse y corregir `.gitignore`
antes de `git add`.

## 6. Preparar y auditar el primer commit

```powershell
git add .
git diff --cached --check
git status --short
```

Bloquear archivos prohibidos:

```powershell
$forbiddenTrackedFiles = @(
    git diff --cached --name-only |
        Where-Object {
            $_ -match '(^|/)\.env$' -or
            $_ -match '^backups/' -or
            $_ -match '^\.codex/' -or
            $_ -match '^\.agents/' -or
            $_ -match '(^|/)node_modules/' -or
            $_ -match '(^|/)target/' -or
            $_ -match '(^|/)dist/'
        }
)

if ($forbiddenTrackedFiles.Count -gt 0) {
    $forbiddenTrackedFiles
    throw "Hay archivos locales o sensibles preparados para commit"
}
```

Buscar formatos de secretos reales dentro de lo preparado:

```powershell
$secretMatches = git grep --cached -n -I -E `
    'CLOUDFLARE_TUNNEL_TOKEN=eyJ|STORAGE_SERVICE_ROLE_KEY=eyJ|JWT_SECRET=[A-Za-z0-9_-]{20,}' `
    2>$null

$secretScanExitCode = $LASTEXITCODE

if ($secretScanExitCode -eq 0) {
    $secretMatches
    throw "Se detectaron posibles secretos en el commit"
}

if ($secretScanExitCode -ne 1) {
    throw "El escaneo de secretos termino con codigo inesperado $secretScanExitCode"
}
```

El codigo `1` de `git grep` significa que no encontro coincidencias y es el
resultado esperado.

Revisar la lista completa:

```powershell
git diff --cached --name-only
```

## 7. Crear el commit y publicar

```powershell
git commit -m "feat: preparar Kontora POS contenedorizado"
git remote add origin <URL-DEL-REPOSITORIO-NUEVO>
git remote -v
git push -u origin main
```

No usar `--force` para resolver un remoto que ya tiene commits. Crear un remoto
vacio o integrar su historial deliberadamente.

## 8. Validar el repositorio publicado

```powershell
git status --short
git log --oneline --decorate -n 3
git ls-files infra/.env
git ls-files backups
git ls-files .codex
```

`git status` debe quedar limpio y los tres `git ls-files` no deben devolver
archivos.

Desde otra carpeta temporal, clonar el remoto y validar que no dependa de
archivos locales:

```powershell
$validationDirectory = Join-Path $env:TEMP 'kontora-repository-validation'

if (Test-Path -LiteralPath $validationDirectory) {
    throw "La carpeta temporal ya existe: $validationDirectory"
}

git clone <URL-DEL-REPOSITORIO-NUEVO> $validationDirectory
Set-Location $validationDirectory

Test-Path infra\.env.production.example
Test-Path infra\compose.prod.yml
Test-Path frontend\vercel.json
```

Los dos primeros resultados deben ser `True`; `frontend\vercel.json` debe ser
`False` porque la arquitectura nueva usa Nginx.

## Si un secreto llega al remoto

Eliminar el archivo en un commit posterior no elimina el valor del historial.
Si se publica un token, contrasena o JWT:

1. rotar inmediatamente la credencial;
2. desconectar replicas del tunnel si se expuso su token;
3. impedir nuevos despliegues con el valor anterior;
4. reemplazar el repositorio o limpiar el historial con una herramienta
   especifica;
5. repetir el escaneo antes de volver a publicar.

La rotacion es obligatoria aunque el repositorio sea privado.
