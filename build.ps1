param([string]$SdkPath = "$env:LOCALAPPDATA\Android\Sdk", [string]$JdkPath = '')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not $JdkPath) { $JdkPath = Split-Path (Split-Path (Get-Command javac -ErrorAction Stop).Source) }
$env:JAVA_HOME = $JdkPath
$tools = Join-Path $SdkPath 'build-tools\36.1.0'
$androidJar = Join-Path $SdkPath 'platforms\android-35\android.jar'
if (-not (Test-Path -LiteralPath $androidJar)) { throw 'Instale Android SDK Platform 35.' }
if (-not (Test-Path -LiteralPath (Join-Path $tools 'aapt2.exe'))) { throw 'Instale Android SDK Build Tools 36.1.0.' }
$build = Join-Path $PSScriptRoot 'build'
$signing = Join-Path $PSScriptRoot 'signing'
New-Item -ItemType Directory -Force -Path $build,(Join-Path $build 'classes'),(Join-Path $build 'generated'),(Join-Path $build 'dex'),$signing | Out-Null
function Check-Exit([string]$step) { if ($LASTEXITCODE -ne 0) { throw "Falha na etapa: $step (código $LASTEXITCODE)." } }
& (Join-Path $tools 'aapt2.exe') compile --dir 'app\src\main\res' -o (Join-Path $build 'resources.zip')
Check-Exit 'compilar recursos'
$inventoryFile = Join-Path $PSScriptRoot 'dependencies\inventory.json'
if (-not (Test-Path -LiteralPath $inventoryFile)) { throw 'Execute python prepare_google.py para preparar as bibliotecas oficiais.' }
$inventory = Get-Content -Raw -LiteralPath $inventoryFile | ConvertFrom-Json
$jars = @(); $overlays = @(); $packages = @()
foreach ($dependency in $inventory) {
    if ($dependency.type -eq 'jar') { $jars += Join-Path $PSScriptRoot ('dependencies\' + $dependency.file); continue }
    $directory = Join-Path $PSScriptRoot ('dependencies\' + $dependency.artifact + '-' + $dependency.version)
    $jar = Join-Path $directory 'classes.jar'
    if (Test-Path -LiteralPath $jar) { $jars += $jar }
    $resourceDirectory = Join-Path $directory 'res'
    if ((Test-Path -LiteralPath $resourceDirectory) -and (Get-ChildItem -LiteralPath $resourceDirectory -Recurse -File | Select-Object -First 1)) {
        $compiled = Join-Path $build ($dependency.artifact + '-res.zip')
        & (Join-Path $tools 'aapt2.exe') compile --dir $resourceDirectory -o $compiled
        Check-Exit ('recursos de ' + $dependency.artifact)
        $overlays += @('-R',$compiled)
    }
    if ($dependency.package) { $packages += $dependency.package }
}
& (Join-Path $tools 'aapt2.exe') link -o (Join-Path $build 'unsigned.apk') -I $androidJar --manifest 'app\src\main\AndroidManifest.xml' --java (Join-Path $build 'generated') -A 'app\src\main\assets' -0 wav --auto-add-overlay --extra-packages ($packages -join ':') (Join-Path $build 'resources.zip') @overlays
Check-Exit 'empacotar recursos'
$sources = @(Get-ChildItem 'app\src\main\java',(Join-Path $build 'generated') -Recurse -Filter '*.java' | ForEach-Object FullName)
& (Join-Path $JdkPath 'bin\javac.exe') -encoding UTF-8 -source 8 -target 8 -bootclasspath "$androidJar;$(Join-Path $tools 'core-lambda-stubs.jar')" -classpath ($jars -join ';') -d (Join-Path $build 'classes') @sources
Check-Exit 'compilar Java'
& (Join-Path $JdkPath 'bin\jar.exe') cf (Join-Path $build 'classes.jar') -C (Join-Path $build 'classes') .
Check-Exit 'empacotar classes'
& (Join-Path $JdkPath 'bin\java.exe') -cp (Join-Path $tools 'lib\d8.jar') com.android.tools.r8.D8 --min-api 26 --lib $androidJar --output (Join-Path $build 'dex') (Join-Path $build 'classes.jar') @jars
Check-Exit 'gerar DEX'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open((Join-Path $build 'unsigned.apk'),[System.IO.Compression.ZipArchiveMode]::Update)
try { foreach ($dex in (Get-ChildItem -LiteralPath (Join-Path $build 'dex') -Filter 'classes*.dex')) { $old = $archive.GetEntry($dex.Name); if ($old) { $old.Delete() }; [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$dex.FullName,$dex.Name) | Out-Null } } finally { $archive.Dispose() }
& (Join-Path $tools 'zipalign.exe') -f 4 (Join-Path $build 'unsigned.apk') (Join-Path $build 'aligned.apk')
Check-Exit 'alinhar APK'
$keystore = Join-Path $signing 'livepulse.p12'
$passwordFile = Join-Path $signing 'password.txt'
if (-not (Test-Path -LiteralPath $keystore)) {
    if (Test-Path -LiteralPath $passwordFile) { throw 'Senha de assinatura existente sem chave. Confira a pasta signing.' }
    $password = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    [IO.File]::WriteAllText($passwordFile,$password,[Text.UTF8Encoding]::new($false))
    & (Join-Path $JdkPath 'bin\keytool.exe') -genkeypair -keystore $keystore -storetype PKCS12 -storepass:file $passwordFile -keypass:file $passwordFile -alias livepulse -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=LivePulse Local, OU=Personal, O=LivePulse, C=BR'
    Check-Exit 'criar chave de assinatura'
}
& (Join-Path $JdkPath 'bin\java.exe') -jar (Join-Path $tools 'lib\apksigner.jar') sign --ks $keystore --ks-key-alias livepulse --ks-pass "file:$passwordFile" --out (Join-Path $build 'LivePulse-2.4.2.apk') (Join-Path $build 'aligned.apk')
Check-Exit 'assinar APK'
& (Join-Path $JdkPath 'bin\java.exe') -jar (Join-Path $tools 'lib\apksigner.jar') verify --verbose (Join-Path $build 'LivePulse-2.4.2.apk')
Check-Exit 'verificar assinatura'
Write-Output ('APK pronto: ' + (Join-Path $build 'LivePulse-2.4.2.apk'))
