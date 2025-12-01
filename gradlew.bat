@ECHO OFF
SET DIR=%~dp0
SET JAVA_EXE=java
IF NOT "%JAVA_HOME%"=="" SET JAVA_EXE=%JAVA_HOME%\bin\java
"%JAVA_EXE%" -version >NUL 2>&1 || (
  ECHO Java not found, please install JDK
  EXIT /B 1
)
"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%DIR%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
