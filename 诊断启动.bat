@echo off
setlocal
cd /d "%~dp0"
title 喵酱桌宠 · 诊断启动（会显示报错）

rem ============================================================
rem  桌宠启动不了的时候用这个。
rem  它和「启动桌宠.bat」唯一的区别是：保留黑窗口并打印日志，
rem  报错信息看得见，方便截图求助。正常使用请用「启动桌宠.bat」。
rem ============================================================

set "JAVAW="
if exist "%~dp0runtime\bin\javaw.exe" set "JAVAW=%~dp0runtime\bin\javaw.exe"
if not defined JAVAW if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAW=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVAW for /f "delims=" %%i in ('where javaw 2^>nul') do if not defined JAVAW set "JAVAW=%%i"
if not defined JAVAW if exist "%USERPROFILE%\.jdks\corretto-1.8.0_512\bin\javaw.exe" set "JAVAW=%USERPROFILE%\.jdks\corretto-1.8.0_512\bin\javaw.exe"
if not defined JAVAW if exist "%USERPROFILE%\.jdks\temurin-17\bin\javaw.exe" set "JAVAW=%USERPROFILE%\.jdks\temurin-17\bin\javaw.exe"
if not defined JAVAW for /d %%d in ("%ProgramFiles%\Java\*") do if not defined JAVAW if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe"
if not defined JAVAW for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\*") do if not defined JAVAW if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe"

if not defined JAVAW (
    echo   [X] 没有找到 Java，请先看 README.md 第 2 节。
    pause
    exit /b 1
)

if not exist "out\pet\Pet.class" (
    echo   [X] 没有编译产物 out\pet\Pet.class，请先双击「重新编译.bat」。
    pause
    exit /b 1
)

set "JAVA=%JAVAW:javaw.exe=java.exe%"

echo   ------------------------------------------------------------
echo    Java 路径: %JAVA%
echo    工作目录:  %CD%
echo    数据库:    %CD%\data\pet.db
echo   ------------------------------------------------------------
echo   正在启动，关闭桌宠窗口后本窗口才会继续……
echo.

"%JAVA%" -cp "out;lib/*" pet.Pet

echo.
echo   ------------------------------------------------------------
echo    桌宠已退出，退出码 %errorlevel%
echo    如果上面出现过 Exception / Error / 找不到 之类的字样，
echo    把整个窗口截图发给懂 Java 的人即可定位问题。
echo   ------------------------------------------------------------
pause
exit /b 0
