# Crear un repositorio Git nuevo

## Objetivo

Retirar el historial anterior, crear una rama `main` limpia y publicar el
proyecto en un repositorio remoto vacío.

Este procedimiento elimina únicamente `.git`. No elimina el código,
`infra/.env`, respaldos ni volúmenes Docker.

## Antes de comenzar

1. Estar ubicado en la raíz `C:\Users\corre\Documentos\kontora`.
2. Tener una copia del proyecto.
3. Crear un repositorio remoto privado y vacío.
4. No agregar README, licencia ni `.gitignore` desde GitHub o GitLab.
5. Confirmar que `.gitignore` contiene al menos:

```gitignore
*.env
backups/
*.dump
*.backup
.tmp/
.codex/
.agents/
node_modules/
target/
dist/
```

## Orden exacto

Si existe un repositorio anterior, eliminar solamente su metadata:

```powershell
Remove-Item -LiteralPath .git -Recurse -Force
```

Si PowerShell informa que `.git` no existe, continuar con el siguiente
comando.

Inicializar y publicar:

```powershell
git init -b main
git add --all
git commit -m "feat: preparar Kontora POS contenedorizado"
git remote add origin <URL-DEL-NUEVO-REPOSITORIO>
git push -u origin main
```

Reemplazar `<URL-DEL-NUEVO-REPOSITORIO>` por la URL HTTPS o SSH real.

Ejecutar los comandos en ese orden. Si uno falla, corregirlo antes de ejecutar
el siguiente. No usar `git push --force`.

## Cambios posteriores

Para publicar una modificación nueva:

```powershell
git add --all
git commit -m "descripcion breve del cambio"
git push
```

## Secretos

Nunca agregar:

- `infra/.env`;
- tokens de Cloudflare;
- contraseñas o JWT;
- respaldos de PostgreSQL;
- archivos del volumen de Storage.

Si un secreto llega al remoto, eliminarlo en un commit posterior no es
suficiente. Se debe rotar inmediatamente y sustituir el valor en el entorno.
