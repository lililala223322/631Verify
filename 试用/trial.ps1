param([switch]$RunSample)

Set-Location -LiteralPath $PSScriptRoot
$javaExe = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe)) { $javaExe = 'java' }
$projectRoot = Split-Path -Parent $PSScriptRoot
while ($true) {
    Write-Host ''
    Write-Host 'CPM 配置验证工具 — 试用入口' -ForegroundColor Cyan
    Write-Host '1. 运行已验证通过的 L1 样例'
    Write-Host '2. 选择自己的 XML 和 BIN 目录进行验证'
    Write-Host '3. 查看工具帮助'
    Write-Host '4. 打开样例结果目录（自选文件结果在 reports 和 logs）'
    Write-Host '0. 退出'
    if ($RunSample) { $choice = '1'; $RunSample = $false } else { $choice = Read-Host '输入序号并按回车' }
    switch ($choice) {
        '1' {
            $xmlFile = Join-Path $PSScriptRoot 'sample\xml\CPM_L1_Module.xml'
            $binFolder = Join-Path $PSScriptRoot 'sample\bin'
            & $javaExe -jar CPM_Gen_Verify.jar $xmlFile $binFolder sample/reports sample/logs -64 -big
            if ($LASTEXITCODE -eq 0) {
                Write-Host '样例验证通过。报告在 sample\reports，日志在 sample\logs。' -ForegroundColor Green
            } else {
                Write-Host "样例验证失败，退出码：$LASTEXITCODE。请查看上方错误信息。" -ForegroundColor Yellow
            }
        }
        '2' {
            $xmlFile = (Read-Host '输入主 XML 文件完整路径').Trim().Trim('"')
            $binFolder = (Read-Host '输入 BIN 文件夹完整路径').Trim().Trim('"')
            $bits = Read-Host '位数：32 或 64（回车默认 64）'
            if ($bits -eq '') { $bits = '64' }
            $endian = Read-Host '字节序：big 或 little（回车默认 big）'
            if ($endian -eq '') { $endian = 'big' }
            if ($bits -notin @('32', '64') -or $endian -notin @('big', 'little')) {
                Write-Host '位数或字节序输入有误。' -ForegroundColor Yellow
                break
            }
            & $javaExe -jar CPM_Gen_Verify.jar $xmlFile $binFolder reports logs "-$bits" "-$endian"
            Write-Host "本次进程退出码：$LASTEXITCODE。"
        }
        '3' { & $javaExe -jar CPM_Gen_Verify.jar -h }
        '4' {
            Start-Process explorer.exe -ArgumentList ('"' + (Join-Path $PSScriptRoot 'sample\reports') + '"')
            Start-Process explorer.exe -ArgumentList ('"' + (Join-Path $PSScriptRoot 'sample\logs') + '"')
        }
        '0' { return }
        default { Write-Host '请输入 0 到 4。' }
    }
}
