# Auto-generated by EclipseNSIS Script Wizard
# Dec 23, 2008 1:39:11 PM

Name "Java Distributed Computing Platform"

# Defines
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION 0.1
!define COMPANY ""
!define URL http://jdcp.googlecode.com

# MUI defines
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKLM
!define MUI_STARTMENUPAGE_NODISABLE
!define MUI_STARTMENUPAGE_REGISTRY_KEY ${REGKEY}
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULTFOLDER JDCP
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE

# Included files
!include Sections.nsh
!include MUI.nsh

# Variables
Var StartMenuGroup

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE LICENSE
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English

# Installer attributes
OutFile build/dist/jdcp-${VERSION}.exe
InstallDir "$PROGRAMFILES\Java Distributed Computing Platform"
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion 0.1.0.0
VIAddVersionKey ProductName "Java Distributed Computing Platform"
VIAddVersionKey ProductVersion "${VERSION}"
VIAddVersionKey CompanyWebsite "${URL}"
VIAddVersionKey FileVersion "${VERSION}"
VIAddVersionKey FileDescription ""
VIAddVersionKey LegalCopyright ""
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

# Installer sections
Section -Main SEC0000
    SetOutPath $INSTDIR
    SetOverwrite on
    File build\dist\jdcp-${VERSION}\jdcp-32.png
    File build\dist\jdcp-${VERSION}\jdcp.ico
    File build\dist\jdcp-${VERSION}\jdcp-common.jar
    SetOutPath $INSTDIR\etc
    File build\dist\jdcp-${VERSION}\etc\log4j.properties
    SetOutPath $INSTDIR\lib
    File build\dist\jdcp-${VERSION}\lib\log4j-1.2.15.jar
    File build\dist\jdcp-${VERSION}\lib\eandb.jar
    File build\dist\jdcp-${VERSION}\lib\derby.jar
    File build\dist\jdcp-${VERSION}\lib\derbyclient.jar
    WriteRegStr HKLM "${REGKEY}\Components" Main 1
SectionEnd

Section /o Server SEC0001
    SetOutPath $INSTDIR
    SetOverwrite on
    File build\dist\jdcp-${VERSION}\jdcp-server.sh
    File build\dist\jdcp-${VERSION}\jdcp-server.bat
    File build\dist\jdcp-${VERSION}\jdcp-server.jar
    SetOutPath $INSTDIR\etc
    File build\dist\jdcp-${VERSION}\etc\policy
    File build\dist\jdcp-${VERSION}\etc\login.config
    File build\dist\jdcp-${VERSION}\etc\passwd
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    SetOutPath $INSTDIR
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\JDCP Server.lnk" "$SYSDIR\javaw.exe" "-Djava.library.path=lib -Djava.security.manager -Djava.security.auth.login.config=etc/login.config -Djava.security.policy=etc/policy -Dlog4j.configuration=file:./etc/log4j.properties -jar jdcp-server.jar" "$INSTDIR\jdcp.ico"
    WriteRegStr HKLM "${REGKEY}\Components" Server 1
SectionEnd

Section Worker SEC0002
    SetOutPath $INSTDIR
    SetOverwrite on
    File build\dist\jdcp-${VERSION}\jdcp-worker.jar
    SetOutPath $INSTDIR\lib
    File build\dist\jdcp-${VERSION}\lib\jnlp.jar
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    SetOutPath $INSTDIR
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\JDCP Worker.lnk" "$SYSDIR\javaw.exe" "-Dlog4j.configuration=file:./etc/log4j.properties -jar jdcp-worker.jar" "$INSTDIR\jdcp.ico"
    WriteRegStr HKLM "${REGKEY}\Components" Worker 1
SectionEnd

Section /o Client SEC0003
    SetOutPath $INSTDIR
    SetOverwrite on
    File build\dist\jdcp-${VERSION}\jdcp-client.jar
    WriteRegStr HKLM "${REGKEY}\Components" Client 1
SectionEnd

Section -post SEC0004
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    SetOutPath $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\Uninstall $(^Name).lnk" $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_END
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

# Uninstaller sections
Section /o -un.Client UNSEC0003
    Delete /REBOOTOK $INSTDIR\jdcp-client.jar
    DeleteRegValue HKLM "${REGKEY}\Components" Client
SectionEnd

Section /o -un.Worker UNSEC0002
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\JDCP Worker.lnk"
    Delete /REBOOTOK $INSTDIR\lib\jnlp.jar
    Delete /REBOOTOK $INSTDIR\jdcp-worker.jar
    DeleteRegValue HKLM "${REGKEY}\Components" Worker
SectionEnd

Section /o -un.Server UNSEC0001
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\JDCP Server.lnk"
    Delete /REBOOTOK $INSTDIR\etc\passwd
    Delete /REBOOTOK $INSTDIR\etc\login.config
    Delete /REBOOTOK $INSTDIR\etc\policy
    Delete /REBOOTOK $INSTDIR\jdcp-server.jar
    Delete /REBOOTOK $INSTDIR\jdcp-server.bat
    Delete /REBOOTOK $INSTDIR\jdcp-server.sh
    DeleteRegValue HKLM "${REGKEY}\Components" Server
SectionEnd

Section /o -un.Main UNSEC0000
    Delete /REBOOTOK $INSTDIR\lib\eandb.jar
    Delete /REBOOTOK $INSTDIR\lib\log4j-1.2.15.jar
    Delete /REBOOTOK $INSTDIR\etc\log4j.properties
    Delete /REBOOTOK $INSTDIR\jdcp-common.jar
    Delete /REBOOTOK $INSTDIR\jdcp.ico
    Delete /REBOOTOK $INSTDIR\jdcp-32.png
    DeleteRegValue HKLM "${REGKEY}\Components" Main
SectionEnd

Section -un.post UNSEC0004
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\Uninstall $(^Name).lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue HKLM "${REGKEY}" StartMenuGroup
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"
    RmDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RmDir /REBOOTOK $INSTDIR
SectionEnd

# Installer functions
Function .onInit
    InitPluginsDir
FunctionEnd

# Uninstaller functions
Function un.onInit
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuGroup
    !insertmacro SELECT_UNSECTION Main ${UNSEC0000}
    !insertmacro SELECT_UNSECTION Server ${UNSEC0001}
    !insertmacro SELECT_UNSECTION Worker ${UNSEC0002}
    !insertmacro SELECT_UNSECTION Client ${UNSEC0003}
FunctionEnd

# Section Descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SEC0001} "An application for hosting JDCP jobs."
!insertmacro MUI_DESCRIPTION_TEXT ${SEC0002} "An application for contributing your spare CPU cycles to a job hosted on a JDCP server."
!insertmacro MUI_DESCRIPTION_TEXT ${SEC0003} "An application for working with and submitting jobs to a remote JDCP server."
!insertmacro MUI_FUNCTION_DESCRIPTION_END
