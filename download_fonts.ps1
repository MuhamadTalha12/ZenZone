$fontDir = "c:\Users\Leo\Desktop\AND\Zenzone\app\src\main\res\font"
if (-not (Test-Path $fontDir)) {
    New-Item -ItemType Directory -Force -Path $fontDir | Out-Null
}
Invoke-WebRequest -Uri "https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-Regular.ttf" -OutFile "$fontDir\poppins_regular.ttf"
Invoke-WebRequest -Uri "https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-Medium.ttf" -OutFile "$fontDir\poppins_medium.ttf"
Invoke-WebRequest -Uri "https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-Bold.ttf" -OutFile "$fontDir\poppins_bold.ttf"

Remove-Item -Recurse -Force "c:\Users\Leo\Desktop\AND\Zenzone\app\src\main\res\values-night" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "c:\Users\Leo\Desktop\AND\Zenzone\app\src\main\java\com\example" -ErrorAction SilentlyContinue
