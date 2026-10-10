@echo off
setlocal

rem Unicode-safe launcher. The PowerShell runner handles Chinese paths and XML_BASE.
set "SCRIPT_DIR=%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run-commercial-test-files.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"

echo.
pause
exit /b %EXIT_CODE%
