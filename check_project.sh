#!/bin/bash

echo "========================================="
echo "貞操日記 Android 專案 - 結構檢查"
echo "========================================="
echo ""

echo "📁 專案檔案統計:"
echo "  Kotlin 檔案: $(find . -name '*.kt' | wc -l)"
echo "  XML 資源檔案: $(find ./app/src/main/res -name '*.xml' 2>/dev/null | wc -l)"
echo "  Gradle 配置: $(find . -name '*.gradle.kts' | wc -l)"
echo ""

echo "✅ 核心檔案檢查:"
files=(
    "build.gradle.kts"
    "settings.gradle.kts"
    "app/build.gradle.kts"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/chastity/diary/MainActivity.kt"
    "app/src/main/java/com/chastity/diary/DiaryApplication.kt"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file"
    else
        echo "  ✗ $file (缺失)"
    fi
done

echo ""
echo "📦 資料夾結構:"
echo "  data/local/entity: $(find app/src/main/java/com/chastity/diary/data/local/entity -name '*.kt' 2>/dev/null | wc -l) 檔案"
echo "  data/local/dao: $(find app/src/main/java/com/chastity/diary/data/local/dao -name '*.kt' 2>/dev/null | wc -l) 檔案"
echo "  data/repository: $(find app/src/main/java/com/chastity/diary/data/repository -name '*.kt' 2>/dev/null | wc -l) 檔案"
echo "  viewmodel: $(find app/src/main/java/com/chastity/diary/viewmodel -name '*.kt' 2>/dev/null | wc -l) 檔案"
echo "  ui/screens: $(find app/src/main/java/com/chastity/diary/ui/screens -name '*.kt' 2>/dev/null | wc -l) 檔案"
echo "  ui/navigation: $(find app/src/main/java/com/chastity/diary/ui/navigation -name '*.kt' 2>/dev/null | wc -l) 檔案"
echo "  ui/theme: $(find app/src/main/java/com/chastity/diary/ui/theme -name '*.kt' 2>/dev/null | wc -l) 檔案"
echo "  domain/model: $(find app/src/main/java/com/chastity/diary/domain/model -name '*.kt' 2>/dev/null | wc -l) 檔案"
echo ""

echo "📄 文檔:"
if [ -f "README.md" ]; then
    echo "  ✓ README.md ($(wc -l < README.md) 行)"
else
    echo "  ✗ README.md"
fi

if [ -f "IMPLEMENTATION_SUMMARY.md" ]; then
    echo "  ✓ IMPLEMENTATION_SUMMARY.md ($(wc -l < IMPLEMENTATION_SUMMARY.md) 行)"
else
    echo "  ✗ IMPLEMENTATION_SUMMARY.md"
fi

echo ""
echo "========================================="
echo "專案檢查完成!"
echo "========================================="
