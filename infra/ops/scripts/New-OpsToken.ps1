param(
    [ValidateSet("OPS_LOCAL_TOKEN", "OPS_DB_PASSWORD", "OPS_EXECUTOR_TOKEN")]
    [string]$Name = "OPS_LOCAL_TOKEN"
)

$ErrorActionPreference = "Stop"

$bytes = New-Object byte[] 32
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $generator.GetBytes($bytes)
}
finally {
    $generator.Dispose()
}

$token = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
Write-Output "$Name=$token"
