$patterns = @(
  'eyJhbGci',
  'AIza',
  'ca-app-pub-',
  '-----BEGIN PRIVATE KEY-----',
  'SUPABASE_SERVICE_ROLE',
  'supabase.key',
  'GOOGLE_SERVICES_JSON',
  'FIREBASE_API_KEY',
  'aws_access_key_id',
  'AKIA',
  'secret_access_key',
  'client_secret',
  'password=',
  'token='
)

foreach ($p in $patterns) {
  Write-Host "---- PATTERN: $p ----"
  Get-ChildItem -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\.git' -and $_.Length -lt 1048576 } |
    Select-String -Pattern $p -SimpleMatch -ErrorAction SilentlyContinue |
    ForEach-Object {
      "$($_.Path):$($_.LineNumber):$($_.Line.Trim())"
    }
}

