@REM Maven Wrapper for IT Access Backend
@echo off

set MAVEN_OPTS=-Xmx512m

REM Download Maven Wrapper if not exists
if not exist .mvn\wrapper\maven-wrapper.jar (
    echo Downloading Maven Wrapper...
    mkdir .mvn\wrapper
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '.mvn\wrapper\maven-wrapper.jar'"
)

set MAVEN_CONFIG=%USERPROFILE%\.m2

java -Dmaven.multiModuleProjectDirectory="%CD%" -cp ".mvn/wrapper/maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*