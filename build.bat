@echo off
echo ==========================================================
echo  Building Student Management System (Java Swing + MySQL)
echo ==========================================================

if not exist bin mkdir bin

echo Compiling Java source files...
javac -d bin -cp "lib/*;src" src/db/*.java src/model/*.java src/dao/*.java src/util/*.java src/ui/*.java src/Main.java

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation finished successfully! Output in 'bin/'
) else (
    echo [ERROR] Compilation failed. Please check errors above.
)
pause

