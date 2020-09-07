@echo off
cd %~dp0..
echo  Transparent Adapter for Sockets
@title Transparent Adapter for Sockets
setLocal EnableDelayedExpansion
set CLASSPATH="conf
for /f "tokens=* delims=" %%a in ('dir "*.jar" /b') do (
   set CLASSPATH=!CLASSPATH!;%%a
)
set CLASSPATH=!CLASSPATH!"

java -Djava.ext.dirs=lib -cp %CLASSPATH% com.leeson.TAS