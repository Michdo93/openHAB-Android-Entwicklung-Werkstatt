@echo off
rem Startet ein Beispiel unter Windows: uebersetzt bei Bedarf und laedt .env:
rem   starte.bat kapitel10_erste_schritte\B01_Verbinden.kt [Argumente]
setlocal EnableDelayedExpansion
set "DATEI=%~1"
if "%DATEI%"=="" (echo Aufruf: starte.bat kapitel10_erste_schritte\B01_Verbinden.kt & exit /b 1)
set "KLASSE=%DATEI:.kt=%"
set "KLASSE=!KLASSE:\=.!"
set "KLASSE=!KLASSE:/=.!Kt"
set "ARGS="
shift
:args
if not "%~1"=="" (set "ARGS=!ARGS! %1" & shift & goto args)
call gradlew.bat -q bereit || exit /b 1
if exist .env for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"
java -cp "build\classes\kotlin\main;build\lib\*" !KLASSE! !ARGS!
