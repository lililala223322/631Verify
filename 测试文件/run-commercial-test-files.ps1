$ErrorActionPreference = 'Stop'

$workspace = Split-Path -Parent $PSScriptRoot
$projectDir = Join-Path $workspace '验证工具源码'
$javaExe = if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    (Get-Command java.exe -ErrorAction Stop).Source
}

Add-Type @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public static class CommercialTestNativePath {
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode)]
    public static extern uint GetShortPathName(string path, StringBuilder buffer, uint capacity);
}
'@

function ConvertTo-ShortPath([string]$path) {
    $buffer = New-Object System.Text.StringBuilder 1024
    $length = [CommercialTestNativePath]::GetShortPathName($path, $buffer, $buffer.Capacity)
    if ($length -eq 0) {
        throw "Could not convert test path to a Windows short path: $path"
    }
    return $buffer.ToString()
}

function Test-CommercialLayout([string]$path) {
    return (Test-Path -LiteralPath (Join-Path $path 'CPM')) -and
           (Test-Path -LiteralPath (Join-Path $path 'tmp')) -and
           (Test-Path -LiteralPath (Join-Path $path 'BIN32')) -and
           (Test-Path -LiteralPath (Join-Path $path 'BIN64'))
}

if ($args.Count -gt 0) {
    $testRoot = [IO.Path]::GetFullPath($args[0])
    if (-not (Test-CommercialLayout $testRoot)) {
        $child = Get-ChildItem -LiteralPath $testRoot -Directory |
            Where-Object { Test-CommercialLayout $_.FullName } |
            Select-Object -First 1
        if ($null -ne $child) {
            $testRoot = $child.FullName
        }
    }
} else {
    $testRoot = $PSScriptRoot
}

$jar = Join-Path $projectDir 'dist\CPM_Gen_Verify.jar'
$xsd = Join-Path $projectDir 'dist\XSD\Partition.xsd'

if (-not (Test-Path -LiteralPath $jar)) {
    throw "No runnable JAR found under $projectDir\dist. Build with: mvn install clean"
}
if (-not (Test-Path -LiteralPath $xsd)) {
    throw "Partition.xsd was not found: $xsd"
}
if ([string]::IsNullOrWhiteSpace($testRoot) -or -not (Test-CommercialLayout $testRoot)) {
    throw 'Could not locate the complete commercial test package.'
}
if (-not (Test-Path -LiteralPath (Join-Path $testRoot 'tmp\coreOS.xml'))) {
    throw "XML_BASE file was not found: $testRoot\tmp\coreOS.xml"
}
if (-not (Test-Path -LiteralPath (Join-Path $testRoot 'tmp\pos.xml'))) {
    throw "XML_BASE file was not found: $testRoot\tmp\pos.xml"
}

$testRootLong = $testRoot
$testRootForJava = ConvertTo-ShortPath $testRootLong

$runTag = Get-Date -Format 'yyyyMMdd_HHmmssfff'
$runRoot = Join-Path $projectDir "_xml_req_run\commercial-full-test\bat-run-$runTag"
$summary = Join-Path $runRoot 'SUMMARY.txt'
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null

@(
    "Complete commercial test run: $runTag"
    "JAR: $jar"
    "JAVA: $javaExe"
    "XSD: $xsd"
    "TEST_ROOT: $testRootLong"
    "TEST_ROOT_FOR_JAVA: $testRootForJava"
    "XML_BASE: $(Join-Path $testRootForJava 'tmp')"
    ''
) | Set-Content -LiteralPath $summary -Encoding UTF8

Set-Location -LiteralPath $projectDir
$total = 0
$failed = 0
$skipped = 0
$xmlBase = Join-Path $testRootForJava 'tmp'

$moduleDirs = Get-ChildItem -LiteralPath (Join-Path $testRootForJava 'CPM') -Directory | Sort-Object Name
foreach ($moduleDir in $moduleDirs) {
    $board = $moduleDir.Name
    $unit = $board -replace '^CPM_', ''
    $moduleXmlLong = Join-Path $moduleDir.FullName ($board + '_Module.xml')

    if (-not (Test-Path -LiteralPath $moduleXmlLong)) {
        Add-Content -LiteralPath $summary -Value "[SKIP] $board - module XML not found"
        $skipped += 2
        continue
    }

    $moduleXml = Join-Path (ConvertTo-ShortPath $moduleDir.FullName) ($board + '_Module.xml')

    foreach ($architecture in @('BIN32', 'BIN64')) {
        $binDir = Join-Path (Join-Path $testRootForJava $architecture) $unit
        $caseName = "$architecture-$board"
        $bitOption = if ($architecture -eq 'BIN32') { '-32' } else { '-64' }
        if (-not (Test-Path -LiteralPath $binDir)) {
            Write-Host "[SKIP] $caseName - BIN directory not found"
            Add-Content -LiteralPath $summary -Value "[SKIP] $caseName - BIN directory not found: $binDir"
            $skipped++
            continue
        }

        $caseRun = Join-Path $runRoot $caseName
        $reportDir = Join-Path $caseRun 'report'
        $logDir = Join-Path $caseRun 'log'
        $console = Join-Path $caseRun 'console.txt'
        New-Item -ItemType Directory -Force -Path $reportDir, $logDir | Out-Null

        $env:XML_BASE = $xmlBase
        $total++
        Write-Host "Running $caseName ..."
        Add-Content -LiteralPath $summary -Value "=== $caseName ==="
        Add-Content -LiteralPath $summary -Value "Module XML: $moduleXml"
        Add-Content -LiteralPath $summary -Value "BIN: $binDir"
        Add-Content -LiteralPath $summary -Value "Generator option: $bitOption"
        Add-Content -LiteralPath $summary -Value "XML_BASE: $xmlBase"

        # Preserve stderr in the case log and continue after a failed case.
        $savedErrorPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & $javaExe -jar $jar $moduleXml $binDir $reportDir $logDir $bitOption > $console 2>&1
            $exitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $savedErrorPreference
        }
        $consoleText = Get-Content -LiteralPath $console -Raw -ErrorAction SilentlyContinue
        $statusMatch = [regex]::Match($consoleText, 'Verification Status : (Passed|Failed)')
        $status = if ($statusMatch.Success) { $statusMatch.Value } else { 'Verification Status: not reached' }
        $codeMatches = [regex]::Matches($consoleText, '\[Error\]\[(\d{4})\]')
        $codes = @($codeMatches | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
        $codeText = if ($codes.Count -gt 0) { $codes -join ',' } else { 'none' }

        Add-Content -LiteralPath $summary -Value "Exit code: $exitCode"
        Add-Content -LiteralPath $summary -Value $status
        Add-Content -LiteralPath $summary -Value "Error codes: $codeText"
        $passed = $exitCode -eq 0 -and $statusMatch.Success -and $statusMatch.Groups[1].Value -eq 'Passed' -and $codes.Count -eq 0
        $result = if ($passed) { 'PASS' } else { 'FAIL' }
        Write-Host "[$result] $caseName - exit=$exitCode; $status; errors=$codeText"
        Add-Content -LiteralPath $summary -Value "Result: $result"
        Add-Content -LiteralPath $summary -Value "Console: $console"
        Add-Content -LiteralPath $summary -Value ''
        if (-not $passed) {
            $failed++
        }
    }
}

Add-Content -LiteralPath $summary -Value "Total runs: $total"
Add-Content -LiteralPath $summary -Value "Failed or incomplete runs: $failed"
Add-Content -LiteralPath $summary -Value "Skipped entries: $skipped"

Write-Host ''
Write-Host 'Complete commercial test run finished.'
Write-Host "Total runs: $total"
Write-Host "Failed or incomplete runs: $failed"
Write-Host "Skipped entries: $skipped"
Write-Host "Summary: $summary"
Write-Host "Test outputs: $runRoot"
if ($failed -gt 0 -or $skipped -gt 0 -or $total -eq 0) { exit 1 }
