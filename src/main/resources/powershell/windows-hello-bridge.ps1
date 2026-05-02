param(
  [Parameter(Mandatory = $true)]
  [ValidateSet("verify")]
  [string]$Action,

  [Parameter(Mandatory = $false)]
  [string]$Message = "Vérification Windows Hello"
)

$ErrorActionPreference = "Stop"

function Emit-Json($obj) {
  $obj | ConvertTo-Json -Compress -Depth 6
}

try {
  $sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value

  Add-Type -AssemblyName System.Runtime.WindowsRuntime | Out-Null
  [Windows.Security.Credentials.UI.UserConsentVerifier, Windows.Security.Credentials.UI, ContentType = WindowsRuntime] | Out-Null

  function AsTask-Generic($op, [Type]$t) {
    # Appelle System.WindowsRuntimeSystemExtensions.AsTask<T>(IAsyncOperation<T>) via réflexion
    $ext = [System.WindowsRuntimeSystemExtensions]
    $m = $ext.GetMethods() | Where-Object {
      $_.Name -eq "AsTask" -and $_.IsGenericMethodDefinition -and $_.GetParameters().Count -eq 1
    } | Select-Object -First 1
    if ($null -eq $m) {
      throw "AsTask<T> introuvable."
    }
    $g = $m.MakeGenericMethod($t)
    return $g.Invoke($null, @($op))
  }

  $availabilityOp = [Windows.Security.Credentials.UI.UserConsentVerifier]::CheckAvailabilityAsync()
  $availabilityTask = AsTask-Generic $availabilityOp ([Windows.Security.Credentials.UI.UserConsentVerifierAvailability])
  $availability = $availabilityTask.Result
  if ($availability -ne [Windows.Security.Credentials.UI.UserConsentVerifierAvailability]::Available) {
    Emit-Json @{
      ok = $false
      sid = $sid
      availability = $availability.ToString()
      result = "Unavailable"
      error = "Windows Hello indisponible: $($availability.ToString())"
    }
    exit 0
  }

  $verifyOp = [Windows.Security.Credentials.UI.UserConsentVerifier]::RequestVerificationAsync($Message)
  $verifyTask = AsTask-Generic $verifyOp ([Windows.Security.Credentials.UI.UserConsentVerificationResult])
  $res = $verifyTask.Result
  $ok = ($res -eq [Windows.Security.Credentials.UI.UserConsentVerificationResult]::Verified)

  Emit-Json @{
    ok = $ok
    sid = $sid
    availability = $availability.ToString()
    result = $res.ToString()
    error = $null
  }
  exit 0
}
catch {
  $msg = $_.Exception.Message
  Emit-Json @{
    ok = $false
    sid = $null
    availability = "Unknown"
    result = "Error"
    error = $msg
  }
  exit 0
}

