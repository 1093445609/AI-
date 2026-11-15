@echo off
setlocal
cd /d "%~dp0"
title 喵酱桌宠 · 启动器

rem ============================================================
rem  喵酱 Java 猫娘桌宠 —— 一键启动
rem  自动按顺序找 Java：项目内 runtime > JAVA_HOME > PATH
rem                     > .jdks 目录 > Program Files
rem  找不到会给提示并停住，不会一闪而过。
rem ============================================================

set "JAVAW="

rem 1) 便携版 Java（项目自带的 runtime 文件夹）
if exist "%~dp0runtime\bin\javaw.exe" set "JAVAW=%~dp0runtime\bin\javaw.exe"

rem 2) 系统环境变量 JAVA_HOME
if not defined JAVAW if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAW=%JAVA_HOME%\bin\javaw.exe"

rem 3) 系统 PATH
if not defined JAVAW for /f "delims=" %%i in ('where javaw 2^>nul') do if not defined JAVAW set "JAVAW=%%i"

rem 4) IDEA / JetBrains 下载的 JDK
if not defined JAVAW if exist "%USERPROFILE%\.jdks\corretto-1.8.0_512\bin\javaw.exe" set "JAVAW=%USERPROFILE%\.jdks\corretto-1.8.0_512\bin\javaw.exe"
if not defined JAVAW if exist "%USERPROFILE%\.jdks\temurin-17\bin\javaw.exe" set "JAVAW=%USERPROFILE%\.jdks\temurin-17\bin\javaw.exe"

rem 5) Program Files 下的常见安装位置
if not defined JAVAW for /d %%d in ("%ProgramFiles%\Java\*") do if not defined JAVAW if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe"
if not defined JAVAW for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\*") do if not defined JAVAW if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe"
if not defined JAVAW for /d %%d in ("%ProgramFiles%\Microsoft\jdk-*") do if not defined JAVAW if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe"
if not defined JAVAW for /d %%d in ("%ProgramFiles%\Amazon Corretto\*") do if not defined JAVAW if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe"

if not defined JAVAW goto :nojava

rem ---------------- 没有编译产物就自动编译一次 ----------------
if exist "out\pet\Pet.class" goto :launch

echo.
echo   第一次运行，正在编译，大约 10 秒，请稍等……
echo.

if not exist "out" mkdir "out"
set "JAVAC=%JAVAW:javaw.exe=javac.exe%"
if not exist "%JAVAC%" goto :nojavac

set "SRCLIST=%TEMP%\pet_src_%RANDOM%.txt"
dir /b /s "pet\*.java" > "%SRCLIST%"
"%JAVAC%" -encoding UTF-8 -cp "lib/*" -d "out" "@%SRCLIST%"
if errorlevel 1 goto :compilefail
del "%SRCLIST%" >nul 2>nul
echo   编译成功！

:launch
start "" "%JAVAW%" -cp "out;lib/*" pet.Pet
exit /b 0

rem ==================== 出错提示 ====================
:nojava
echo.
echo   ============================================================
echo    [X] 这台电脑上没有找到 Java
echo   ============================================================
echo.
echo    桌宠是用 Java 写的，必须先有 Java 才能跑。两种办法：
echo.
echo    【办法一】免安装便携版（不改系统，推荐）
echo      1. 浏览器打开 https://adoptium.net/zh-CN/temurin/releases/
echo      2. Version 选 8，Operating System 选 Windows，
echo         Architecture 选 x64，Package Type 选 JRE，点 .zip 下载
echo      3. 解压，把里面那一层文件夹（形如 jdk8u452-b09-jre）整个改名成 runtime
echo      4. 把 runtime 文件夹放到本文件夹里，和 pet、lib、assets 平排
echo      5. 确认这个文件存在：runtime\bin\javaw.exe
echo      6. 再双击本文件
echo.
echo    【办法二】安装到系统里（需要管理员权限）
echo      下载 Temurin 8 的 .msi 安装包，一路下一步，
echo      安装时务必勾上 "Set JAVA_HOME variable" 和 "Add to PATH"，
echo      装完关掉本窗口重新双击。
echo.
echo    详细步骤见 README.md 第 2 节。
echo.
pause
exit /b 1

:nojavac
echo   [X] 找到了运行环境（javaw）但没有编译器（javac）。
echo       说明装的是 JRE，而项目里缺少已编译的 out 目录。
echo       解决：改用 JDK 8 或更高版本，或向对方索取完整的 out 文件夹。
echo.
pause
exit /b 1

:compilefail
echo.
echo   [X] 编译失败。上面那几行英文就是具体报错，截图发给懂 Java 的人即可。
echo       最常见原因：源码被改坏了，或者 Java 版本低于 8。
echo.
pause
exit /b 1
