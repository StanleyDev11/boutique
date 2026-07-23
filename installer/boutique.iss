; =====================================================================
;  Boutique - Script d'installation Inno Setup
; ---------------------------------------------------------------------
;  Ce script produit un installateur Windows PER-USER (sans droits admin)
;  qui installe :
;    - le runtime Java embarque (dossier runtime\ avec javaw.exe),
;    - le fat jar Spring Boot (boutique-1.0.0-SNAPSHOT.jar),
;    - l'icone de l'application (icon.ico),
;  et cree des raccourcis (menu Demarrer + bureau) qui lancent l'app
;  directement via javaw.exe (PAS de lanceur natif jpackage => on evite
;  totalement le bug "Failed to launch JVM").
;
;  PREREQUIS : avoir genere le dossier "payload\" au prealable via le
;  script  installer\build-payload.ps1  (voir README).
;
;  COMPILATION : ouvrir ce fichier dans Inno Setup puis Build > Compile.
;  L'installateur est produit dans  installer\Output\Boutique-Setup-1.0.0.exe
;
;  A ADAPTER si besoin : MyAppName / MyAppVersion / MyJarName ci-dessous.
; =====================================================================

#define MyAppName "Boutique"
#define MyAppVersion "1.0.0"
#define MyPublisher "Boutique"
; Nom EXACT du fat jar present dans payload\ (garder synchronise avec le pom)
#define MyJarName "boutique-1.0.0-SNAPSHOT.jar"

[Setup]
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyPublisher}
; Nom affiche dans "Applications installees" / Panneau de configuration
AppVerName={#MyAppName} {#MyAppVersion}

; --- Installation PER-USER : aucun droit administrateur requis ---
PrivilegesRequired=lowest
; Dossier d'installation par defaut (per-user, inscriptible sans admin)
DefaultDirName={localappdata}\Programs\{#MyAppName}
; Pas de page "groupe du menu Demarrer" (on gere les raccourcis nous-memes)
DisableProgramGroupPage=yes

; --- Sortie ---
OutputDir=Output
OutputBaseFilename=Boutique-Setup-{#MyAppVersion}

; --- Icone de l'installateur et de l'entree de desinstallation ---
; (icon.ico est a la racine du projet, donc un niveau au-dessus de ce .iss)
SetupIconFile=..\icon.ico
UninstallDisplayIcon={app}\icon.ico
UninstallDisplayName={#MyAppName} {#MyAppVersion}

; --- Compression ---
Compression=lzma2/max
SolidCompression=yes

; --- 64 bits ---
ArchitecturesInstallIn64BitMode=x64compatible

WizardStyle=modern

[Languages]
Name: "french"; MessagesFile: "compiler:Languages\French.isl"

[Tasks]
; Raccourci bureau optionnel (case a cocher pendant l'installation)
Name: "desktopicon"; Description: "Creer un raccourci sur le Bureau"; GroupDescription: "Raccourcis :"

[Files]
; Runtime Java embarque -> {app}\runtime  (recursif)
Source: "payload\runtime\*"; DestDir: "{app}\runtime"; Flags: recursesubdirs createallsubdirs ignoreversion
; Fat jar Spring Boot -> {app}
Source: "payload\{#MyJarName}"; DestDir: "{app}"; Flags: ignoreversion
; Icone de l'application -> {app}
Source: "..\icon.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
; Raccourci menu Demarrer
Name: "{userprograms}\{#MyAppName}"; Filename: "{app}\runtime\bin\javaw.exe"; \
    Parameters: "-Dspring.profiles.active=desktop -jar ""{app}\{#MyJarName}"""; \
    WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"; Comment: "Lancer {#MyAppName}"

; Raccourci bureau (si la tache desktopicon est cochee)
Name: "{userdesktop}\{#MyAppName}"; Filename: "{app}\runtime\bin\javaw.exe"; \
    Parameters: "-Dspring.profiles.active=desktop -jar ""{app}\{#MyJarName}"""; \
    WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"; Comment: "Lancer {#MyAppName}"; \
    Tasks: desktopicon

[Run]
; Proposer de lancer l'application a la fin de l'installation
Filename: "{app}\runtime\bin\javaw.exe"; \
    Parameters: "-Dspring.profiles.active=desktop -jar ""{app}\{#MyJarName}"""; \
    WorkingDir: "{app}"; Description: "Lancer {#MyAppName} maintenant"; \
    Flags: nowait postinstall skipifsilent

; NOTE : la base de donnees H2 est creee dans %USERPROFILE%\.boutika et
; N'EST PAS supprimee a la desinstallation (les donnees sont conservees).
