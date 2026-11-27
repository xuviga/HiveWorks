$ErrorActionPreference = 'Stop'
$java = 'C:\Users\XuViGaN\tools\jdk25\jdk-25.0.5.1+2\bin\java.exe'
$dir = 'D:\HDD\MineCraft\smoke-folia'
$log = Join-Path $dir 'logs\latest.log'

function Read-Shared([string]$path) {
    try {
        $stream = [System.IO.File]::Open($path, 'Open', 'Read', 'ReadWrite')
        try {
            $reader = New-Object System.IO.StreamReader($stream)
            return $reader.ReadToEnd()
        } finally {
            $stream.Dispose()
        }
    } catch {
        return ''
    }
}

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $java
$psi.Arguments = '-Xms512M -Xmx1G -jar server.jar --nogui'
$psi.WorkingDirectory = $dir
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true

$p = New-Object System.Diagnostics.Process
$p.StartInfo = $psi
$null = $p.Start()

$outTask = $p.StandardOutput.ReadToEndAsync()
$errTask = $p.StandardError.ReadToEndAsync()

$started = (Get-Date)
$done = $false
try {
    $deadline = $started.AddSeconds(180)
    while ((Get-Date) -lt $deadline -and -not $done) {
        Start-Sleep -Seconds 2
        if ($p.HasExited) { break }
        if (Test-Path $log) {
            $item = Get-Item $log
            if ($item.LastWriteTime -gt $started) {
                $txt = Read-Shared $log
                if ($txt -match 'Done \(') { $done = $true }
            }
        }
    }

    if (-not $done) {
        Write-Output 'SMOKE: server did not reach Done, stopping anyway'
    }

    Start-Sleep -Seconds 3
    try {
        $p.StandardInput.WriteLine('execute in minecraft:overworld run forceload add 0 0')
        $p.StandardInput.Flush()
    } catch {
        Write-Output 'SMOKE: could not write forceload to stdin'
    }
    Start-Sleep -Seconds 4
    try {
        $p.StandardInput.WriteLine('execute in minecraft:overworld run setblock 8 65 8 minecraft:beehive')
        $p.StandardInput.WriteLine('execute in minecraft:overworld run setblock 9 65 8 minecraft:dandelion')
        $p.StandardInput.WriteLine('hw admin stats')
        $p.StandardInput.Flush()
    } catch {
        Write-Output 'SMOKE: could not write commands to stdin'
    }
    Start-Sleep -Seconds 45

    try {
        $p.StandardInput.WriteLine('stop')
        $p.StandardInput.Flush()
    } catch {
        Write-Output 'SMOKE: could not write stop to stdin'
    }

    if (-not $p.WaitForExit(120000)) {
        $p.Kill()
        Write-Output 'SMOKE: killed after timeout'
    }
    $p.WaitForExit()
} finally {
    if (-not $p.HasExited) {
        $p.Kill()
    }
    $out = ''
    $err = ''
    try {
        $out = $outTask.Result
    } catch {
        Write-Output ('SMOKE: stdout capture failed: ' + $_.Exception.Message)
    }
    try {
        $err = $errTask.Result
    } catch {
        Write-Output ('SMOKE: stderr capture failed: ' + $_.Exception.Message)
    }
    [System.IO.File]::WriteAllText((Join-Path $dir 'smoke-run.out'), $out)
    [System.IO.File]::WriteAllText((Join-Path $dir 'smoke-run.err'), $err)
    Write-Output ('SMOKE: exit=' + $p.ExitCode + ' done=' + $done + ' outLen=' + $out.Length)
}
