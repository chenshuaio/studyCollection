param(
    [string]$BaseUrl = 'http://127.0.0.1:18080',
    [string]$MySqlUser = 'root',
    [string]$MySqlPassword = 'root',
    [string]$Database = 'study_collection'
)

$ErrorActionPreference = 'Stop'
$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$username = "codex_exam_rule_$stamp"
$ruleName = "Codex Exam Rule E2E $stamp"
$questionPrefix = "Codex Exam Rule E2E $stamp"
$sessionId = $null
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

function Assert-Condition {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw $Message
    }
}

try {
    $admin = Invoke-StudyApi -Method POST -Path '/auth/login' -Body @{
        username = 'admin'
        password = 'admin123'
    }
    $adminHeaders = @{ Authorization = "Bearer $($admin.token)" }

    $user = Invoke-StudyApi -Method POST -Path '/auth/register' -Body @{
        username = $username
        password = 'Codex123456'
        displayName = 'Codex Exam Rule User'
    }
    Assert-Condition ($user.role -eq 'USER') 'Registered account must be a normal user.'
    $userHeaders = @{ Authorization = "Bearer $($user.token)" }

    $questions = @(
        @{ title = "$questionPrefix SC-B`nA. 1`nB. 0"; type = 'SINGLE_CHOICE'; difficulty = 'BEGINNER'; answer = 'B' },
        @{ title = "$questionPrefix SC-I`nA. int`nB. String"; type = 'SINGLE_CHOICE'; difficulty = 'INTERMEDIATE'; answer = 'A' },
        @{ title = "$questionPrefix MC-B`nA. JVM`nB. HTML`nC. JDK"; type = 'MULTIPLE_CHOICE'; difficulty = 'BEGINNER'; answer = 'AC' },
        @{ title = "$questionPrefix MC-I`nA. List`nB. CSS`nC. Set"; type = 'MULTIPLE_CHOICE'; difficulty = 'INTERMEDIATE'; answer = 'AC' }
    )
    $answerById = @{}
    foreach ($question in $questions) {
        $createdQuestion = Invoke-StudyApi -Method POST -Path '/questions' -Headers $adminHeaders -Body @{
            title = $question.title
            type = $question.type
            difficulty = $question.difficulty
            knowledgePoint = 'Codex Exam Rule Knowledge'
            answer = $question.answer
            analysis = 'Codex E2E analysis.'
        }
        $answerById[[string]$createdQuestion.id] = $question.answer
    }

    $payload = @{
        name = $ruleName
        description = 'Codex real MySQL exam rule verification.'
        durationMinutes = 25
        totalQuestions = 4
        knowledgePoints = @('Codex Exam Rule Knowledge')
        typeQuotas = @{ SINGLE_CHOICE = 2; MULTIPLE_CHOICE = 2 }
        difficultyQuotas = @{ BEGINNER = 2; INTERMEDIATE = 2 }
    }
    $createdRule = Invoke-StudyApi -Method POST -Path '/exam-rules' -Headers $adminHeaders -Body $payload
    $ruleId = $createdRule.id
    Assert-Condition ($createdRule.status -eq 'DRAFT') 'A new exam rule must start as DRAFT.'

    $beforePublish = @(Invoke-StudyApi -Method GET -Path '/exam-rules' -Headers $userHeaders)
    Assert-Condition (-not ($beforePublish.id -contains $ruleId)) 'Draft rule leaked into the public list.'

    $published = Invoke-StudyApi -Method POST -Path "/exam-rules/$ruleId/publish" -Headers $adminHeaders -Body @{}
    Assert-Condition ($published.status -eq 'PUBLISHED') 'Publishing the rule failed.'

    $publicRules = @(Invoke-StudyApi -Method GET -Path '/exam-rules' -Headers $userHeaders)
    Assert-Condition ($publicRules.id -contains $ruleId) 'Published rule is missing from the user list.'

    $session = Invoke-StudyApi -Method POST -Path "/exam-rules/$ruleId/start" -Headers $userHeaders -Body @{}
    $sessionId = $session.id
    $sessionQuestions = @($session.questions)
    Assert-Condition ($sessionQuestions.Count -eq 4) 'Generated simulation exam does not contain four questions.'
    Assert-Condition ((@($sessionQuestions | Where-Object { $_.type -eq 'SINGLE_CHOICE' })).Count -eq 2) 'Single-choice quota mismatch.'
    Assert-Condition ((@($sessionQuestions | Where-Object { $_.type -eq 'MULTIPLE_CHOICE' })).Count -eq 2) 'Multiple-choice quota mismatch.'
    Assert-Condition ((@($sessionQuestions | Where-Object { $_.difficulty -eq 'BEGINNER' })).Count -eq 2) 'Beginner quota mismatch.'
    Assert-Condition ((@($sessionQuestions | Where-Object { $_.difficulty -eq 'INTERMEDIATE' })).Count -eq 2) 'Intermediate quota mismatch.'
    Assert-Condition ((@($sessionQuestions | Where-Object { $_.correctAnswer })).Count -eq 0) 'Correct answers leaked before submission.'

    foreach ($question in $sessionQuestions) {
        $answer = $answerById[[string]$question.id]
        Invoke-StudyApi -Method PUT -Path "/exams/$sessionId/answers/$($question.id)" -Headers $userHeaders -Body @{
            answer = $answer
        } | Out-Null
    }

    $submitted = Invoke-StudyApi -Method POST -Path "/exams/$sessionId/submit" -Headers $userHeaders -Body @{}
    Assert-Condition ($submitted.status -eq 'SUBMITTED') 'Simulation exam was not submitted.'
    Assert-Condition ($submitted.score -eq 40) 'Simulation exam score is incorrect.'
    Assert-Condition ($submitted.totalScore -eq 40) 'Simulation exam total score is incorrect.'

    $history = @(Invoke-StudyApi -Method GET -Path '/exams' -Headers $userHeaders)
    Assert-Condition ($history.id -contains $sessionId) 'Submitted simulation exam is missing from history.'

    Invoke-StudyApi -Method DELETE -Path "/exam-rules/$ruleId" -Headers $adminHeaders | Out-Null
    $ruleId = $null

    [ordered]@{
        result = 'PASS'
        questionCount = $sessionQuestions.Count
        singleChoice = @($sessionQuestions | Where-Object { $_.type -eq 'SINGLE_CHOICE' }).Count
        multipleChoice = @($sessionQuestions | Where-Object { $_.type -eq 'MULTIPLE_CHOICE' }).Count
        beginner = @($sessionQuestions | Where-Object { $_.difficulty -eq 'BEGINNER' }).Count
        intermediate = @($sessionQuestions | Where-Object { $_.difficulty -eq 'INTERMEDIATE' }).Count
        score = "$($submitted.score)/$($submitted.totalScore)"
    } | ConvertTo-Json -Compress
}
finally {
    $escapedUser = $username.Replace("'", "''")
    $escapedRule = $ruleName.Replace("'", "''")
    $escapedPrefix = $questionPrefix.Replace("'", "''")
    $cleanup = @"
DELETE FROM learning_attempts WHERE user_id IN (SELECT id FROM users WHERE username = '$escapedUser');
DELETE FROM exam_sessions WHERE user_id IN (SELECT id FROM users WHERE username = '$escapedUser');
DELETE FROM exam_rules WHERE name = '$escapedRule';
DELETE FROM users WHERE username = '$escapedUser';
DELETE FROM questions WHERE title LIKE '$escapedPrefix%';
"@
    & mysql "-u$MySqlUser" "-p$MySqlPassword" --default-character-set=utf8mb4 $Database -e $cleanup
}
