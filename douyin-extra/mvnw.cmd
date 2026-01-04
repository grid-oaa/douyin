@echo off
REM Maven wrapper script for Windows
REM Sets JAVA_HOME to Java 17 before running Maven

set JAVA_HOME=D:\development-kit\jdk-17.0.11
set PATH=%JAVA_HOME%\bin;%PATH%

mvn %*
