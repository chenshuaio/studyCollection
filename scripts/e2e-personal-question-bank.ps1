param(
    [string]$BaseUrl = 'http://127.0.0.1:18080',
    [string]$MySqlUser = 'root',
    [string]$MySqlPassword = 'root',
    [string]$Database = 'study_collection'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$usernameA = "codex_personal_a_$stamp"
$usernameB = "codex_personal_b_$stamp"
$questionPrefix = "Codex Personal Bank $stamp"
$ruleName = "Codex Personal Rule $stamp"
$userIds = @()
$questionIds = @()
$pendingIds = @()
$ruleId = $null

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

function Invoke-StudyApiExpectFailure {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [hashtable]$Headers
    )

    $client = [System.Net.Http.HttpClient]::new()
    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method),
        "$BaseUrl$Path"
    )
    foreach ($header in $Headers.GetEnumerator()) {
        $request.Headers.TryAddWithoutValidation($header.Key, [string]$header.Value) | Out-Null
    }
    $json = $Body | ConvertTo-Json -Depth 12 -Compress
    $request.Content = [System.Net.Http.StringContent]::new(
        $json,
        [System.Text.Encoding]::UTF8,
        'application/json'
    )
    try {
        $httpResponse = $client.SendAsync($request).GetAwaiter().GetResult()
        $raw = $httpResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if ($httpResponse.IsSuccessStatusCode) {
            throw "Expected API $Method $Path to fail."
        }
        $response = $raw | ConvertFrom-Json
        Assert-Condition ($response.code -eq 'VALIDATION_FAILED') "Unexpected error code for $Method $Path."
        Assert-Condition (-not [string]::IsNullOrWhiteSpace($response.message)) "Missing error message for $Method $Path."
        return $response
    } finally {
        $request.Dispose()
        $client.Dispose()
    }
}

function Assert-Condition {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw $Message
    }
}

function Join-Ids {
    param([object[]]$Values)
    $normalized = @($Values | Where-Object { $null -ne $_ } | ForEach-Object { [long]$_ })
    if ($normalized.Count -eq 0) {
        return '0'
    }
    return $normalized -join ','
}

try {
    $admin = Invoke-StudyApi -Method POST -Path '/auth/login' -Body @{
        username = 'admin'
        password = 'admin123'
    }
    $adminHeaders = @{ Authorization = "Bearer $($admin.token)" }

    $userA = Invoke-StudyApi -Method POST -Path '/auth/register' -Body @{
        username = $usernameA
        password = 'Codex123456'
        displayName = 'Personal Bank A'
    }
    $userB = Invoke-StudyApi -Method POST -Path '/auth/register' -Body @{
        username = $usernameB
        password = 'Codex123456'
        displayName = 'Personal Bank B'
    }
    $userIds = @($userA.userId, $userB.userId)
    Assert-Condition ($userA.role -eq 'USER' -and $userB.role -eq 'USER') 'Registered accounts must be normal users.'
    $headersA = @{ Authorization = "Bearer $($userA.token)" }
    $headersB = @{ Authorization = "Bearer $($userB.token)" }

    $personalPending = Invoke-StudyApi -Method POST -Path '/questions/pending' -Headers $headersA -Body @{
        title = "$questionPrefix Personal`nA. private`nB. public"
        type = 'SINGLE_CHOICE'
        difficulty = 'BEGINNER'
        knowledgePoint = $questionPrefix
        answer = 'A'
        analysis = 'Personal question analysis.'
        targetScope = 'PERSONAL'
    }
    $publicPending = Invoke-StudyApi -Method POST -Path '/questions/pending' -Headers $headersA -Body @{
        title = "$questionPrefix Public`nA. wrong`nB. correct"
        type = 'SINGLE_CHOICE'
        difficulty = 'BEGINNER'
        knowledgePoint = $questionPrefix
        answer = 'B'
        analysis = 'Public question analysis.'
        targetScope = 'PUBLIC'
    }
    $pendingIds = @($personalPending.id, $publicPending.id)
    Assert-Condition ($personalPending.targetScope -eq 'PERSONAL') 'Personal pending scope was not saved.'
    Assert-Condition ($publicPending.targetScope -eq 'PUBLIC') 'Public pending scope was not saved.'

    $pending = @(Invoke-StudyApi -Method GET -Path '/questions/pending' -Headers $adminHeaders)
    Assert-Condition (($pending.id -contains $personalPending.id) -and ($pending.id -contains $publicPending.id)) 'Administrator cannot see both pending submissions.'

    $personalQuestion = Invoke-StudyApi -Method POST -Path "/questions/pending/$($personalPending.id)/approve" -Headers $adminHeaders -Body @{}
    $publicQuestion = Invoke-StudyApi -Method POST -Path "/questions/pending/$($publicPending.id)/approve" -Headers $adminHeaders -Body @{}
    $questionIds = @($personalQuestion.id, $publicQuestion.id)
    Assert-Condition ($personalQuestion.ownerUserId -eq $userA.userId) 'Personal question owner mismatch.'
    Assert-Condition ($null -eq $publicQuestion.ownerUserId) 'Public question must not have an owner.'

    $visibleToA = @(Invoke-StudyApi -Method GET -Path "/questions?keyword=$([uri]::EscapeDataString($questionPrefix))&scope=ALL" -Headers $headersA)
    $visibleToB = @(Invoke-StudyApi -Method GET -Path "/questions?keyword=$([uri]::EscapeDataString($questionPrefix))&scope=ALL" -Headers $headersB)
    $personalToA = @(Invoke-StudyApi -Method GET -Path "/questions?keyword=$([uri]::EscapeDataString($questionPrefix))&scope=PERSONAL" -Headers $headersA)
    Assert-Condition ($visibleToA.Count -eq 2) 'Owner should see public and personal questions.'
    Assert-Condition ($visibleToB.Count -eq 1 -and $visibleToB[0].id -eq $publicQuestion.id) 'Other user saw a foreign personal question.'
    Assert-Condition ($personalToA.Count -eq 1 -and $personalToA[0].id -eq $personalQuestion.id) 'Personal scope did not return the owner question.'
    Assert-Condition ((@($visibleToA | Where-Object { $_.answer -or $_.analysis })).Count -eq 0) 'Question answers leaked from normal user search.'

    $personalPractice = Invoke-StudyApi -Method POST -Path '/practice/generate' -Headers $headersA -Body @{
        knowledgePoint = $questionPrefix
        difficulty = 'BEGINNER'
        type = 'SINGLE_CHOICE'
        count = 5
        scope = 'PERSONAL'
    }
    Assert-Condition ($personalPractice.actualCount -eq 1) 'Personal practice did not contain exactly one question.'
    Assert-Condition ($personalPractice.questions[0].id -eq $personalQuestion.id) 'Personal practice selected the wrong question.'

    $practiceResult = Invoke-StudyApi -Method POST -Path '/practice/submit' -Headers $headersA -Body @{
        answers = @(@{ questionId = $personalQuestion.id; answer = 'A' })
    }
    Assert-Condition ($practiceResult.score -eq 10) 'Owner could not submit the personal practice question.'

    $customExam = Invoke-StudyApi -Method POST -Path '/exams/custom' -Headers $headersA -Body @{
        name = "Personal Exam $stamp"
        durationMinutes = 20
        questionIds = @($personalQuestion.id)
    }
    Assert-Condition ($customExam.questions.Count -eq 1) 'Owner could not create a personal-question exam.'

    Invoke-StudyApi -Method POST -Path '/questions/feedback' -Headers $headersA -Body @{
        questionId = $personalQuestion.id
        type = 'STEM_ERROR'
        content = 'Owner feedback for personal question.'
        submittedAnswer = 'A'
        sourceContext = 'QUESTION_DETAIL'
        sourceReference = "personal-$stamp"
    } | Out-Null

    Invoke-StudyApiExpectFailure -Method POST -Path '/practice/submit' -Headers $headersB -Body @{
        answers = @(@{ questionId = $personalQuestion.id; answer = 'A' })
    } | Out-Null
    Invoke-StudyApiExpectFailure -Method POST -Path '/exams/custom' -Headers $headersB -Body @{
        name = "Forbidden Exam $stamp"
        durationMinutes = 20
        questionIds = @($personalQuestion.id)
    } | Out-Null
    Invoke-StudyApiExpectFailure -Method POST -Path '/questions/feedback' -Headers $headersB -Body @{
        questionId = $personalQuestion.id
        type = 'STEM_ERROR'
        content = 'Foreign feedback attempt.'
        submittedAnswer = 'A'
        sourceContext = 'QUESTION_DETAIL'
        sourceReference = "forbidden-$stamp"
    } | Out-Null
    Invoke-StudyApiExpectFailure -Method POST -Path '/mistakes' -Headers $headersB -Body @{
        questionId = $personalQuestion.id
        submittedAnswer = 'B'
        sourceContext = 'PRACTICE'
    } | Out-Null

    $rule = Invoke-StudyApi -Method POST -Path '/exam-rules' -Headers $adminHeaders -Body @{
        name = $ruleName
        description = 'Must only use the public question.'
        durationMinutes = 20
        totalQuestions = 1
        knowledgePoints = @($questionPrefix)
        typeQuotas = @{ SINGLE_CHOICE = 1 }
        difficultyQuotas = @{ BEGINNER = 1 }
    }
    $ruleId = $rule.id
    Invoke-StudyApi -Method POST -Path "/exam-rules/$ruleId/publish" -Headers $adminHeaders -Body @{} | Out-Null
    $simulation = Invoke-StudyApi -Method POST -Path "/exam-rules/$ruleId/start" -Headers $headersB -Body @{}
    Assert-Condition ($simulation.questions.Count -eq 1) 'Simulation exam question count mismatch.'
    Assert-Condition ($simulation.questions[0].id -eq $publicQuestion.id) 'Simulation exam used a personal question.'

    Invoke-StudyApi -Method DELETE -Path "/exam-rules/$ruleId" -Headers $adminHeaders | Out-Null
    $ruleId = $null

    [ordered]@{
        result = 'PASS'
        ownerVisibleCount = $visibleToA.Count
        otherVisibleCount = $visibleToB.Count
        personalPracticeQuestionId = $personalPractice.questions[0].id
        personalExamQuestionCount = $customExam.questions.Count
        simulationQuestionId = $simulation.questions[0].id
        accessBypassChecks = 4
    } | ConvertTo-Json -Compress
}
finally {
    $userIdList = Join-Ids $userIds
    $questionIdList = Join-Ids $questionIds
    $pendingIdList = Join-Ids $pendingIds
    $ruleIdList = Join-Ids @($ruleId)
    $cleanup = @"
DELETE FROM question_revisions WHERE question_id IN ($questionIdList);
DELETE FROM question_feedback WHERE question_id IN ($questionIdList) OR user_id IN ($userIdList);
DELETE FROM mistake_records WHERE user_id IN ($userIdList) OR question_id IN ($questionIdList);
DELETE FROM learning_attempts WHERE user_id IN ($userIdList) OR question_id IN ($questionIdList);
DELETE FROM learning_reports WHERE user_id IN ($userIdList);
DELETE FROM practice_stats WHERE user_id IN ($userIdList);
DELETE FROM exam_sessions WHERE user_id IN ($userIdList);
DELETE FROM exam_rules WHERE id IN ($ruleIdList) OR name = '$ruleName';
DELETE FROM pending_questions WHERE id IN ($pendingIdList) OR submitter_user_id IN ($userIdList);
DELETE FROM questions WHERE id IN ($questionIdList);
DELETE FROM users WHERE id IN ($userIdList);
"@
    & mysql "-u$MySqlUser" "-p$MySqlPassword" --default-character-set=utf8mb4 $Database -e $cleanup
}
