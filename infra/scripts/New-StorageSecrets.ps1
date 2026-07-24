[CmdletBinding()]
param(
    [ValidateRange(1, 20)]
    [int]$ValidityYears = 10
)

$ErrorActionPreference = "Stop"

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)

    $base64 = [Convert]::ToBase64String($Bytes)
    return $base64.TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function ConvertTextTo-Base64Url {
    param([string]$Value)

    return ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($Value))
}

$secretBytes = New-Object byte[] 48
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $random.GetBytes($secretBytes)
} finally {
    $random.Dispose()
}
$jwtSecret = ConvertTo-Base64Url $secretBytes

function New-StorageJwt {
    param([string]$Role)

    $now = [DateTimeOffset]::UtcNow
    $header = ConvertTextTo-Base64Url '{"alg":"HS256","typ":"JWT"}'
    $payloadObject = [ordered]@{
        role = $Role
        iss  = "kontora-storage"
        iat  = $now.ToUnixTimeSeconds()
        exp  = $now.AddYears($ValidityYears).ToUnixTimeSeconds()
    }
    $payload = ConvertTextTo-Base64Url ($payloadObject | ConvertTo-Json -Compress)
    $unsignedToken = "$header.$payload"

    $hmac = New-Object Security.Cryptography.HMACSHA256
    try {
        $hmac.Key = [Text.Encoding]::UTF8.GetBytes($jwtSecret)
        $signature = ConvertTo-Base64Url (
            $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsignedToken))
        )
    } finally {
        $hmac.Dispose()
    }

    return "$unsignedToken.$signature"
}

Write-Output "STORAGE_JWT_SECRET=$jwtSecret"
Write-Output "STORAGE_SERVICE_ROLE_KEY=$(New-StorageJwt -Role 'service_role')"
Write-Output ""
Write-Output "# Valores generados localmente. No los publique ni los reutilice entre entornos."
