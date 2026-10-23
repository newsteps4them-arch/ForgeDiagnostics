' Start Forge Continuous Sync Daemon silently (No console window)
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
rootDir = fso.GetParentFolderName(scriptDir)

WshShell.CurrentDirectory = rootDir
WshShell.Run "node scripts/auto_sync.js", 0, False
