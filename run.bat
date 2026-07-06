@echo off
pushd "%~dp0"

echo [1/4] Compilando con Java...
java -jar target\lyc-compiler-3.0.0.jar src\main\resources\input\test.txt
if errorlevel 1 (
    echo ERROR: Fallo la compilacion Java.
    popd
    pause
    exit /b 1
)

echo [2/4] Copiando archivos ASM...
COPY /Y target\output\final.asm src\main\resources\asm\final.asm >nul
COPY /Y src\main\resources\asm\macros2.asm target\asm\macros2.asm >nul
COPY /Y src\main\resources\asm\number.asm  target\asm\number.asm  >nul

echo [3/4] Ensamblando y ejecutando en DOSBox...
(
    echo [autoexec]
    echo mount c "%CD%\src\main\resources\asm"
    echo c:
    echo run.bat
    echo exit
) > dosbox_temp.conf

"C:\Program Files (x86)\DOSBox-0.74-3\DOSBox.exe" -conf dosbox_temp.conf
del dosbox_temp.conf

echo [4/4] Listo.
popd
