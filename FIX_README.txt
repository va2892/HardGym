HardGym v0.1.1 compile fix for Yarn 1.16.5+build.10

Copy the included src folder into your existing HardGym_v0.1_source folder and allow Windows to replace the two Java files.
Keep your existing gradle/, gradlew, gradlew.bat and gradle/wrapper/gradle-wrapper.properties.

Then run from PowerShell in the project folder:
  .\gradlew.bat clean build
