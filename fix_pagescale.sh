#!/bin/bash
set -e
BASE="app/src/main/java/com/aliothmoon/maameow"

# 1. ThemeSettingsViewMaterial - 删残余函数和 import
sed -i "/pageScale/d" "$BASE/ui/screen/settings/ThemeSettingsViewMaterial.kt"
sed -i "/setUiPageScale/d" "$BASE/ui/screen/settings/ThemeSettingsViewMaterial.kt"
sed -i "/uiPageScale/d" "$BASE/ui/screen/settings/ThemeSettingsViewMaterial.kt"
sed -i "/mutableFloatStateOf/d" "$BASE/ui/screen/settings/ThemeSettingsViewMaterial.kt"

# 2. SettingsViewModel - 删字段和函数
sed -i "/val uiPageScale/d" "$BASE/ui/viewmodel/SettingsViewModel.kt"
sed -i "/fun setUiPageScale/d" "$BASE/ui/viewmodel/SettingsViewModel.kt"
sed -i "/appSettingsManager.uiPageScale/d" "$BASE/ui/viewmodel/SettingsViewModel.kt"
sed -i "/appSettingsManager.setUiPageScale/d" "$BASE/ui/viewmodel/SettingsViewModel.kt"

# 3. AppSettingsManager - 删 StateFlow 和 setter
sed -i "/val uiPageScale/d" "$BASE/data/preferences/AppSettingsManager.kt"
sed -i "/fun setUiPageScale/d" "$BASE/data/preferences/AppSettingsManager.kt"

# 4. MainActivity - 删 usage
sed -i "/val pageScale/d" "$BASE/MainActivity.kt"
sed -i "/scaledDensity/d" "$BASE/MainActivity.kt"

# 5. AppSettings.kt - 删属性
sed -i "/uiPageScale/d" "$BASE/domain/models/AppSettings.kt"

echo '=== PageScale Removed ==='
