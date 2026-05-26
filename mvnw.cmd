@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
set MAVEN_WRAPPER_PROPERTIES="%~dp0.mvn\wrapper\maven-wrapper.properties"

for /f "usebackq tokens=1,2 delims==" %%a in (%MAVEN_WRAPPER_PROPERTIES%) do (
    if "%%a"=="distributionUrl" set DISTRIBUTION_URL=%%b
)

set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if not exist %MAVEN_WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    java -cp "" ^
        "-Dmaven.multiModuleProjectDirectory=%~dp0" ^
        org.apache.maven.wrapper.MavenWrapperDownloader ^
        %DISTRIBUTION_URL% 2>nul
    if not exist %MAVEN_WRAPPER_JAR% (
        echo Downloading maven-wrapper.jar from Maven Central...
        powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%~dp0.mvn\wrapper\maven-wrapper.jar'"
    )
)

java %MAVEN_OPTS% ^
    "-Dmaven.multiModuleProjectDirectory=%~dp0" ^
    -jar %MAVEN_WRAPPER_JAR% %*

endlocal
