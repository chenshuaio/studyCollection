param(
    [string]$BaseUrl = 'http://127.0.0.1:18080',
    [string]$MySqlUser = 'root',
    [string]$MySqlPassword = 'root',
    [string]$Database = 'study_collection'
)

$ErrorActionPreference = 'Stop'
$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$username = "codex_ai_$stamp"
$questionTitle = "Codex AI Audit Question $stamp"
$fakeApiKey = 'codex-e2e-ai-key'
$fakeServerProcess = $null
$userId = $null
$questionId = $null
$auditBaseline = 0L
$settingsBackup = $null

function Assert-Condition {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-StudyApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $arguments = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $Headers
        ContentType = 'application/json; charset=utf-8'
    }
    if ($null -ne $Body) {
        $arguments.Body = $Body | ConvertTo-Json -Depth 12 -Compress
    }
    $response = Invoke-RestMethod @arguments
    if ($response.code -ne 'OK') {
        throw "API $Method $Path failed: $($response.message)"
    }
    return $response.data
}

function Invoke-MySql {
    param([string]$Sql)

    $mysqlArguments = @(
        "--user=$MySqlUser",
        "--password=$MySqlPassword",
        '--default-character-set=utf8mb4',
        '--batch',
        '--skip-column-names',
        $Database,
        '--execute',
        $Sql
    )
    $output = & mysql @mysqlArguments
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed with exit code $LASTEXITCODE."
    }
    return $output
}

function Find-FreePort {
    $listener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback,
        0
    )
    $listener.Start()
    try {
        return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    } finally {
        $listener.Stop()
    }
}

function Start-FakeOpenAiServer {
    param([int]$Port, [string]$ExpectedApiKey)

    $helperPath = Join-Path $PSScriptRoot 'fake-openai-test-server.ps1'
    $startArguments = @{
        FilePath = 'powershell.exe'
        ArgumentList = @(
            '-NoProfile',
            '-ExecutionPolicy',
            'Bypass',
            '-File',
            $helperPath,
            '-Port',
            $Port,
            '-ExpectedApiKey',
            $ExpectedApiKey
        )
        WindowStyle = 'Hidden'
        PassThru = $true
    }
    return Start-Process @startArguments
}

function Restore-AiSettings {
    if ([string]::IsNullOrWhiteSpace($settingsBackup)) {
        Invoke-MySql -Sql 'DELETE FROM ai_model_settings WHERE id = 1' | Out-Null
        return
    }

    $parts = $settingsBackup -split "`t"
    Assert-Condition ($parts.Count -eq 5) 'Invalid AI settings backup.'
    $restoreSql = @"
INSERT INTO ai_model_settings (id, provider, endpoint, model_name, updated_by, updated_at)
VALUES (
  1,
  CONVERT(FROM_BASE64('$($parts[0])') USING utf8mb4),
  CONVERT(FROM_BASE64('$($parts[1])') USING utf8mb4),
  CONVERT(FROM_BASE64('$($parts[2])') USING utf8mb4),
  $($parts[3]),
  '$($parts[4])'
)
ON DUPLICATE KEY UPDATE
  provider = VALUES(provider),
  endpoint = VALUES(endpoint),
  model_name = VALUES(model_name),
  updated_by = VALUES(updated_by),
  updated_at = VALUES(updated_at)
"@
    Invoke-MySql -Sql $restoreSql | Out-Null
}

try {
    $auditValue = Invoke-MySql -Sql 'SELECT COALESCE(MAX(id), 0) FROM ai_call_audits'
    if ($auditValue) {
        $auditBaseline = [long]$auditValue
    }
    $settingsValue = Invoke-MySql -Sql @"
SELECT TO_BASE64(provider), TO_BASE64(endpoint), TO_BASE64(model_name), updated_by,
       DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s')
FROM ai_model_settings
WHERE id = 1
"@
    if ($settingsValue) {
        $settingsBackup = [string]$settingsValue
    }

    $fakePort = Find-FreePort
    $fakeServerProcess = Start-FakeOpenAiServer -Port $fakePort -ExpectedApiKey $fakeApiKey
    Start-Sleep -Milliseconds 600
    Assert-Condition (-not $fakeServerProcess.HasExited) 'Fake OpenAI server failed to start.'

    $env:STUDY_COLLECTION_AI_API_KEY = $fakeApiKey
    & "$PSScriptRoot\stop-local.ps1"
    & "$PSScriptRoot\start-local.ps1" -UseMysql

    $admin = Invoke-StudyApi -Method POST -Path '/auth/login' -Body @{
        username = 'admin'
        password = 'admin123'
    }
    $adminHeaders = @{ Authorization = "Bearer $($admin.token)" }

    $user = Invoke-StudyApi -Method POST -Path '/auth/register' -Body @{
        username = $username
        password = 'Codex123456'
        displayName = 'AI E2E User'
    }
    $userId = [long]$user.userId
    $userHeaders = @{ Authorization = "Bearer $($user.token)" }

    $fakeEndpoint = "http://127.0.0.1:$fakePort/v1/chat/completions"
    $settings = Invoke-StudyApi -Method PUT -Path '/ai/settings' -Headers $adminHeaders -Body @{
        endpoint = $fakeEndpoint
        modelName = 'codex-e2e-model'
    }
    Assert-Condition ($settings.apiKeyConfigured -eq $true) 'AI API key should be configured from the environment.'
    Assert-Condition ($settings.modelName -eq 'codex-e2e-model') 'AI model settings were not saved.'

    $connection = Invoke-StudyApi -Method POST -Path '/ai/settings/test' -Headers $adminHeaders -Body @{}
    Assert-Condition ($connection.success -eq $true) 'AI connection test should succeed.'
    Assert-Condition ($connection.source -eq 'ONLINE_MODEL') 'AI connection test should use the online model.'

    $question = Invoke-StudyApi -Method POST -Path '/questions' -Headers $adminHeaders -Body @{
        title = "$questionTitle`nA. JVM heap`nB. Native method stack"
        type = 'SINGLE_CHOICE'
        difficulty = 'BEGINNER'
        knowledgePoint = 'JVM'
        answer = 'A'
        analysis = 'The JVM heap stores object instances.'
    }
    $questionId = [long]$question.id

    Invoke-StudyApi -Method POST -Path '/practice/submit' -Headers $userHeaders -Body @{
        answers = @(@{ questionId = $questionId; answer = 'A' })
    } | Out-Null

    $onlineReport = Invoke-StudyApi -Method POST -Path '/reports/learning' -Headers $userHeaders -Body @{
        mode = 'ONLINE_MODEL'
        revisedQuestionPolicy = 'EXCLUDE_REVISED'
    }
    Assert-Condition ($onlineReport.adviceSource -eq 'ONLINE_MODEL') 'Online report should use the fake model.'
    Assert-Condition ($onlineReport.adviceContent -like '*review JVM memory*') 'Online report did not contain fake model advice.'

    $successfulAudits = @(Invoke-StudyApi -Method GET -Path '/ai/audits?limit=50' -Headers $adminHeaders)
    $newSuccessfulAudits = @($successfulAudits | Where-Object { [long]$_.id -gt $auditBaseline })
    Assert-Condition (@($newSuccessfulAudits | Where-Object { $_.purpose -eq 'CONFIG_TEST' -and $_.status -eq 'SUCCESS' }).Count -eq 1) 'Missing successful CONFIG_TEST audit.'
    Assert-Condition (@($newSuccessfulAudits | Where-Object { $_.purpose -eq 'LEARNING_REPORT' -and $_.status -eq 'SUCCESS' }).Count -eq 1) 'Missing successful LEARNING_REPORT audit.'

    Invoke-StudyApi -Method PUT -Path '/ai/settings' -Headers $adminHeaders -Body @{
        endpoint = 'http://127.0.0.1:1/v1/chat/completions'
        modelName = 'codex-e2e-model'
    } | Out-Null
    $fallbackReport = Invoke-StudyApi -Method POST -Path '/reports/learning' -Headers $userHeaders -Body @{
        mode = 'ONLINE_MODEL'
        revisedQuestionPolicy = 'EXCLUDE_REVISED'
    }
    Assert-Condition ($fallbackReport.adviceSource -eq 'RULES') 'Unavailable online model should fall back to rules.'
    Assert-Condition (-not [string]::IsNullOrWhiteSpace($fallbackReport.adviceContent)) 'Fallback report should contain rule advice.'

    $allAudits = @(Invoke-StudyApi -Method GET -Path '/ai/audits?limit=50' -Headers $adminHeaders)
    $fallbackAudit = $allAudits | Where-Object {
        [long]$_.id -gt $auditBaseline -and $_.purpose -eq 'LEARNING_REPORT' -and $_.status -eq 'FALLBACK'
    } | Select-Object -First 1
    Assert-Condition ($null -ne $fallbackAudit) 'Missing fallback LEARNING_REPORT audit.'
    Assert-Condition (-not [string]::IsNullOrWhiteSpace($fallbackAudit.failureReason)) 'Fallback audit should contain a failure reason.'
    Assert-Condition ($fallbackAudit.failureReason -notlike "*$fakeApiKey*") 'Fallback audit leaked the API key.'

    [pscustomobject]@{
        result = 'PASS'
        userId = $userId
        questionId = $questionId
        successAuditCount = @($newSuccessfulAudits | Where-Object { $_.status -eq 'SUCCESS' }).Count
        fallbackAuditId = $fallbackAudit.id
        onlineAdviceSource = $onlineReport.adviceSource
        fallbackAdviceSource = $fallbackReport.adviceSource
    } | ConvertTo-Json -Compress
} finally {
    if ($null -ne $userId) {
        $cleanupSql = @"
DELETE FROM learning_reports WHERE user_id = $userId;
DELETE FROM learning_attempts WHERE user_id = $userId;
DELETE FROM practice_stats WHERE user_id = $userId;
DELETE FROM mistake_records WHERE user_id = $userId;
DELETE FROM question_feedback WHERE user_id = $userId;
DELETE FROM users WHERE id = $userId;
"@
        Invoke-MySql -Sql $cleanupSql | Out-Null
    }
    if ($null -ne $questionId) {
        Invoke-MySql -Sql "DELETE FROM questions WHERE id = $questionId" | Out-Null
    }
    Invoke-MySql -Sql "DELETE FROM ai_call_audits WHERE id > $auditBaseline" | Out-Null
    Restore-AiSettings
    if ($null -ne $fakeServerProcess -and -not $fakeServerProcess.HasExited) {
        Stop-Process -Id $fakeServerProcess.Id -Force -ErrorAction SilentlyContinue
    }
}
