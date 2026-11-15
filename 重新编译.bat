@echo off
setlocal
cd /d "%~dp0"
title 喵酱桌宠 · 重新编译

rem ============================================================
rem  改了 pet 目录下的 .java 源码之后，双击本文件重新编译。
rem  编译产物输出到 out 目录，然后双击「启动桌宠.bat」运行。
rem ============================================================

set "JAVAW="
if exist "%~dp0runtime\bin\javaw.exe" set "JAVAW=%~dp0runtime\bin\javaw.exe"
if not defined JAVAW if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAW=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVAW for /f "delims=" %%i in ('where javaw 2^>nul') do if not defined JAVAW set "JAVAW=%%i"
if not defined JAVAW if exist "%USERPROFILE%\.jdks\corretto-1.8.0_512\bin\javaw.exe" set "JAVAW=%USERPROFILE%\.jdks\corretto-1.8.0_512\bin\javaw.exe"
if not defined JAVAW if exist "%USERPROFILE%\.jdks\temurin-17\bin\javaw.exe" set "JAVAW=%USERPROFILE%\.jdks\temurin-17\bin\javaw.exe"
if not defined JAVAW for /d %%d in ("%ProgramFiles%\Java\*") do if not defined JAVAW if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe"
if not defined JAVAW for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\*") do if not defined JAVAW if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe"

if not defined JAVAW goto :nojava

set "JAVAC=%JAVAW:javaw.exe=javac.exe%"
if not exist "%JAVAC%" goto :nojavac

echo.
echo   [1/2] 正在清理旧的 out 目录……
if exist "out" rd /s /q "out"
mkdir "out"

echo   [2/2] 正在编译全部源码……
set "SRCLIST=%TEMP%\pet_src_%RANDOM%.txt"
dir /b /s "pet\*.java" > "%SRCLIST%"
"%JAVAC%" -encoding UTF-8 -cp "lib/*" -d "out" "@%SRCLIST%"
if errorlevel 1 goto :fail
del "%SRCLIST%" >nul 2>nul

echo.
echo   ============================================================
echo    编译成功！现在可以双击「启动桌宠.bat」运行。
echo   ============================================================
echo.
pause
exit /b 0

:nojava
echo.
echo   [X] 没有找到 Java，无法编译。
echo       请先按 README.md 第 2 节装好 JDK 8 或更高版本。
echo.
pause
exit /b 1

:nojavac
echo.
echo   [X] 只找到运行环境（javaw），没找到编译器（javac）。
echo       编译需要 JDK（不是 JRE），请安装 JDK 8 或更高版本。
echo.
pause
exit /b 1

:fail
echo.
echo   ============================================================
echo    [X] 编译失败，请看上面的英文报错。
echo        常见原因：源码改出语法错误、Java 版本低于 8。
echo   ============================================================
echo.
pause
exit /b 1
