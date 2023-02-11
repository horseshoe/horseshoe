# Horseshoe

Horseshoe is written entirely in Java and uses the gradle build system. The exact dependencies needed can be found in [Airpower.ps1](Airpower.ps1) and the [Dockerfile](Dockerfile)

## Windows (Airpower)

### Install Airpower

The Airpower package manager can be used to install all dependencies needed to build Horseshoe using PowerShell on Windows. Use the following commands to install Airpower:

```pwsh
Install-PackageProvider -Name NuGet -Force
Set-PSRepository -Name PSGallery -InstallationPolicy Trusted
Install-Module Airpower -Scope CurrentUser
```

### Build

```pwsh
Airpower exec -ScriptBlock { ./gradlew build }
```

## Linux

```sh
./gradlew build
```
