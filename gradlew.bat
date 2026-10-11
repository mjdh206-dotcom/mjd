@echo off
REM Gradle Wrapper for Windows
REM Usage: gradlew.bat assembleDebug

if "%JAVA_HOME%" == "" (
    set JAVACMD=java
) else (
    set JAVACMD=%JAVA_HOME%\bin\java
)

%JAVACMD% -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
