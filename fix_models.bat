@echo off
REM Fix TheFourPrimitivesAndWeaponsModModels.java by removing Modelplayer_slim references

set "FILE=src\main\java\the_four_primitives_and_weapons\init\TheFourPrimitivesAndWeaponsModModels.java"

echo Fixing %FILE%...

REM Create backup
copy "%FILE%" "%FILE%.backup" >nul

REM Remove the import line
powershell -Command "(Get-Content '%FILE%') -replace 'import the_four_primitives_and_weapons\.client\.model\.Modelplayer_slim;', '// REMOVED: import the_four_primitives_and_weapons.client.model.Modelplayer_slim; // File does not exist' | Set-Content '%FILE%'"

REM Remove the registration line
powershell -Command "(Get-Content '%FILE%') -replace '\t\tevent\.registerLayerDefinition\(Modelplayer_slim\.LAYER_LOCATION, Modelplayer_slim::createBodyLayer\);', '// REMOVED: event.registerLayerDefinition(Modelplayer_slim.LAYER_LOCATION, Modelplayer_slim::createBodyLayer); // File does not exist' | Set-Content '%FILE%'"

echo Fixed %FILE%
echo Backup saved as %FILE%.backup
