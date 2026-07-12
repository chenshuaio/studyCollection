param(
    [Parameter(Mandatory = $true)]
    [int]$Port,
    [Parameter(Mandatory = $true)]
    [string]$ExpectedApiKey
)

$ErrorActionPreference = 'Stop'
$listener = [System.Net.Sockets.TcpListener]::new(
    [System.Net.IPAddress]::Loopback,
    $Port
)
$listener.Start()

try {
    while ($true) {
        $client = $listener.AcceptTcpClient()
        try {
            $stream = $client.GetStream()
            $headerBytes = [System.Collections.Generic.List[byte]]::new()
            while ($true) {
                $nextByte = $stream.ReadByte()
                if ($nextByte -lt 0) {
                    break
                }
                $headerBytes.Add([byte]$nextByte)
                $count = $headerBytes.Count
                $headerComplete = $count -ge 4 -and $headerBytes[$count - 4] -eq 13 -and $headerBytes[$count - 3] -eq 10 -and $headerBytes[$count - 2] -eq 13 -and $headerBytes[$count - 1] -eq 10
                if ($headerComplete) {
                    break
                }
            }
            $headerText = [System.Text.Encoding]::ASCII.GetString($headerBytes.ToArray())
            $authorizationMatch = [regex]::Match($headerText, '(?im)^Authorization:\s*(.+)\r?$')
            $authorization = if ($authorizationMatch.Success) { $authorizationMatch.Groups[1].Value.Trim() } else { '' }
            $lengthMatch = [regex]::Match($headerText, '(?im)^Content-Length:\s*(\d+)\r?$')
            $contentLength = if ($lengthMatch.Success) { [int]$lengthMatch.Groups[1].Value } else { 0 }
            for ($index = 0; $index -lt $contentLength; $index++) {
                if ($stream.ReadByte() -lt 0) {
                    break
                }
            }

            if ($authorization -eq "Bearer $ExpectedApiKey") {
                $status = '200 OK'
                $body = '{"choices":[{"message":{"role":"assistant","content":"Online model advice: review JVM memory and complete two focused practices."}}]}'
            } else {
                $status = '401 Unauthorized'
                $body = '{"error":{"message":"unauthorized"}}'
            }

            $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
            $headers = "HTTP/1.1 $status`r`nContent-Type: application/json; charset=utf-8`r`nContent-Length: $($bodyBytes.Length)`r`nConnection: close`r`n`r`n"
            $headerBytes = [System.Text.Encoding]::ASCII.GetBytes($headers)
            $stream.Write($headerBytes, 0, $headerBytes.Length)
            $stream.Write($bodyBytes, 0, $bodyBytes.Length)
            $stream.Flush()
        } catch {
            # Keep the fake server alive for the next independent request.
        } finally {
            $client.Dispose()
        }
    }
} finally {
    $listener.Stop()
}
