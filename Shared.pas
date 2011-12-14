UNIT Shared;

INTERFACE

USES
  Windows, SysUtils, Classes;

CONST
  TXT_APP    = 'ImageJ';
  CFG_EXT    = '.cfg';

  JAR_IMAGEJ = 'ij.jar';
  JAR_TOOLS  = 'tools.jar';
  DIR_TOOLS  = 'lib\'+JAR_TOOLS;

  EXE_JVM    = 'javaw.exe';

  // Command Line Params
  CLP_CLPTH  = '-cp';
  CLP_IJPATH = '-ijpath';
  CLP_MEM    = '-Xmx';
  CLP_MEMFMT = CLP_MEM+'%dm';

  CLS_IMGJ   = 'ij.ImageJ';

  DEF_CFG    = TXT_APP+CFG_EXT;
  DEF_JVM    = 'bin\'+EXE_JVM;
  DEF_JRE    = 'jre\'+DEF_JVM; // ImageJ installed
  DEF_MEM    = 640;

  NFG_JVM1   = '1.4.2'; // Exclude this if found

  REG_JRE    = '\SOFTWARE\JavaSoft\Java Runtime Environment';
  REG_JDK    = '\SOFTWARE\JavaSoft\Java Development Kit';
  REG_CUR    = 'CurrentVersion';
  REG_JHM    = 'JavaHome';

// Program functions
procedure FatalError(sMsg,sTitle: String; nErrCode: Integer);
function GetValidIJJar(var sIJDir,sIJJar: String; bDotFmt: Boolean): Boolean;
function GetValidJVM(sIJDir: String; var sJVMFile: String): Boolean;
function RelativeJVM(sIJDir,sJVM: String): String;
function CreateMemParam(sParams: String): String;
function CreateClassPath(sDefJar: String; stlCP: TStringList): String;
function ExtractClassPath(sIJJar,sParams: String; stlCP: TStringList): Integer;
procedure AdjustJVMParams(sIJJar,sIJDir: String; var sParams: String);
function CreateCfgFile(sDefCfg,sDefDir,sDefJVM,sDefParams,sDefJar: String;
  bPrompt: Boolean): String;
function ReadCfgFile(sCfgFile: String; var sGetDir,sGetJVM,sGetParams: String): Boolean;

// General utility functions
function EvalSet(bEval: Boolean; var bResult: Boolean): Boolean; overload;
function EvalSet(sEval: String; var sResult: String): String; overload;
function EvalSet(nEval: Integer; var nResult: Integer): Integer; overload;
function GetShortPathString(sLongPathName: String): String;
function GetSpecialFolderPath(nFolder: Integer; bTrailingSlash: Boolean = true): String;
function GetCmdLnParams: String;
function NormalizeDir(sPath: String; bTrailingSlash: Boolean = true): String;
function PosNc(sSubStr,sStr: String): Integer;

IMPLEMENTATION

uses
  ActiveX, ShlObj, Registry, OpenDlg;

// -----------------------------------------------------------------------

// Display error message and exit with error code
procedure FatalError(sMsg,sTitle: String; nErrCode: Integer);
begin
  MessageBox(0,PChar(sMsg),PChar(sTitle),MB_OK or MB_ICONERROR);
  halt(nErrCode);
end;

function GetValidIJJar(var sIJDir,sIJJar: String; bDotFmt: Boolean): Boolean;
begin
  // Default to no JAR found
  Result:=false;
  // Setup default IJ dir
  if (EvalSet(Trim(sIJDir),sIJDir) = '') or (sIJDir = '.') then
    sIJDir:=ExtractFilePath(ParamStr(0));
  sIJDir:=NormalizeDir(sIJDir);
  // Setup IJ jar
  if (EvalSet(Trim(sIJJar),sIJJar) = '') then
    sIJJar:=sIJDir+JAR_IMAGEJ
  else
    if (Trim(ExtractFilePath(sIJJar)) = '') then
      sIJJar:=sIJDir+sIJJar;
  // If no file, then try default install folder...
  if not(FileExists(sIJJar)) then
    begin
      sIJJar:=GetSpecialFolderPath(CSIDL_PROGRAM_FILES)+TXT_APP+'\'+JAR_IMAGEJ;
      // If no file, then setup default search location
      if not(FileExists(sIJJar)) then
        sIJJar:=GetSpecialFolderPath(CSIDL_PROGRAM_FILES)+JAR_IMAGEJ;
    end;
  // Could not find JAR file - give the user some feedback, then prompt
  if not(FileExists(sIJJar)) then
    begin
      MessageBox(0,'A valid '+TXT_APP+' JAR file could not be located. You will now '+
                 'be prompted for the location of a JAR file ('+JAR_IMAGEJ+'). If you '+
                 'cancel the following dialog, you will not be able to run '+TXT_APP+'.',
                 'JAR Not Found',MB_OK or MB_ICONINFORMATION);
      if not(OpenDialog(0,'Select the '+TXT_APP+' JAR File',ExtractFilePath(sIJJar),
                        TXT_APP+' ('+JAR_IMAGEJ+')|'+JAR_IMAGEJ+
                        '|Jar Files (*.jar)|*.jar|All Files (*.*)|*.*',sIJJar)) then
        exit;
    end;
  // Check file again here since this may have been set by OpenDialog above...
  if FileExists(sIJJar) then
    begin
      sIJDir:=ExtractFilePath(sIJJar);
      if bDotFmt and SameText(sIJDir,ExtractFilePath(ParamStr(0))) then
        sIJDir:='.';
      sIJJar:=GetShortPathString(sIJJar);
      Result:=true;
    end;
end;

function GetValidJVM(sIJDir: String; var sJVMFile: String): Boolean;
var
  reg: TRegistry;
  sJVM: String;

function GetJPath(sRegPath: String; var sPath: String): Boolean;
var
  sVersion: String;
begin
  Result:=false;
  if reg.OpenKey(REG_JRE,false) then
    try
      sPath:=reg.ReadString(REG_CUR);
      reg.CloseKey;
      if reg.OpenKey(NormalizeDir(sRegPath)+sPath,false) then
        begin
          sPath:=NormalizeDir(reg.ReadString(REG_JHM))+DEF_JVM;
          // Exclude NFG_JVM1 here (list could be longer)
          Result:=(Pos(NFG_JVM1,sPath) = 0) and FileExists(sPath);
          reg.CloseKey;
        end;
    except
      reg.CloseKey;
    end;
end;

begin
  // Check if file exists outright
  if EvalSet(FileExists(sJVMFile),Result) then // Sets Result value here
    exit;
  // Check if it is in a relative path
  if (sIJDir = '') or (sIJDir = '.') then
    sIJDir:=ExtractFilePath(ParamStr(0));
  sIJDir:=NormalizeDir(sIJDir);
  if (Pos(':',sJVMFile) = 0) and FileExists(sIJDir+sJVMFile) then
    begin
      sJVMFile:=sIJDir+sJVMFile; // Expand the path
      Result:=true;
      exit;
    end;
  // It doesn't exist and is not in a relative path - default to ImageJ JRE
  if FileExists(sIJDir+DEF_JRE) then
    begin
      sJVMFile:=sIJDir+DEF_JRE;
      Result:=true;
      exit;
    end;
  // Still nothing - check the registry to auto-detect
  try
    reg:=TRegistry.Create;
    reg.RootKey:=HKEY_LOCAL_MACHINE;
    // Get the JRE location - removed "choice" logic here
    // Now it ALWAYS defaults to JDK (Wayne's request :-)
    //
    // Try JDK first
    if GetJPath(REG_JDK,sJVM) then
      begin
        sJVMFile:=sJVM;
        Result:=true;
        exit;
      end;
    // Try JRE next
    if GetJPath(REG_JRE,sJVM) then
      begin
        sJVMFile:=sJVM;
        Result:=true;
        exit;
      end;
  finally
    reg.Free;
  end;
  // Could not find JVM in registry - give the user some feedback, then prompt
  MessageBox(0,'A valid Java Virtual Machine (JVM) could not be located in either '+
             'the '+TXT_APP+' folder or Windows registry (JDK/JRE). Please select '+
             'the location of a JVM ('+EXE_JVM+') to use with '+TXT_APP+'. If you '+
             'cancel the following dialog, you will not be able to run '+TXT_APP+
             ' until a valid JVM has been installed.','JVM Not Found',
             MB_OK or MB_ICONINFORMATION);
  sJVMFile:=GetSpecialFolderPath(CSIDL_PROGRAM_FILES)+EXE_JVM;
  Result:=OpenDialog(0,'Select a JVM for '+TXT_APP,ExtractFilePath(sJVMFile),
                     'JVM ('+EXE_JVM+')|'+EXE_JVM+'|All Files (*.*)|*.*',sJVMFile);
end;

function RelativeJVM(sIJDir,sJVM: String): String;
begin
  if (EvalSet(Trim(sIJDir),sIJDir) = '') or (sIJDir = '.') then
    sIJDir:=ExtractFilePath(ParamStr(0))
  else
    sIJDir:=NormalizeDir(sIJDir);
  sJVM:=Trim(sJVM);
  if (PosNc(sIJDir,sJVM) = 1) then
    Result:=Copy(sJVM,Length(sIJDir)+1,Length(sJVM))
  else
    Result:=sJVM;
end;

function CreateMemParam(sParams: String): String;
var
  mst: TMemoryStatus;
  dwMem: DWORD;
  nPos: Integer;
begin
  Result:='';
  if (sParams <> '') and (EvalSet(Pos(CLP_MEM,sParams),nPos) <> 0) then
    while (nPos <= Length(sParams)) and (sParams[nPos] <> ' ') do
      begin
        Result:=Result+sParams[nPos];
        Inc(nPos);
      end;
  // Default to 2/3 available physical memory (in MB) or DEF_MEM, whichever is SMALLER 
  if (Result = '') then
    begin
      mst.dwLength:=SizeOf(mst);
      GlobalMemoryStatus(mst);
      dwMem:=(mst.dwTotalPhys div $300000)*2;
      if (dwMem > DEF_MEM) then
        dwMem:=DEF_MEM;
      Result:=Format(CLP_MEMFMT,[dwMem]);
    end;
end;

function CreateClassPath(sDefJar: String; stlCP: TStringList): String;
var
  i: Integer;
begin
  if (stlCP.Count = 0) then
    Result:=sDefJar
  else
    begin
      Result:=stlCP[0];
      for i:=1 to (stlCP.Count-1) do
        Result:=Result+';'+stlCP[i];
    end;
  if (Pos(' ',Result) <> 0) then
    Result:='"'+Result+'"';
end;

// Returns > 0 if sIJJar is in the ClassPath of sParams
function ExtractClassPath(sIJJar,sParams: String; stlCP: TStringList): Integer;
var
  stl: TStringList;
  sCP: String;
  bQuoted: Boolean;
  nPos: Integer;
begin
  Result:=-1;
  stl:=TStringList.Create;
  try
    sCP:='';
    sIJJar:=Trim(sIJJar);
    sParams:=Trim(sParams);
    if (EvalSet(PosNc(CLP_CLPTH,sParams),nPos) = 0) then
      exit;
    nPos:=nPos+Length(CLP_CLPTH);
    while (nPos <= Length(sParams)) and (sParams[nPos] = ' ') do
      Inc(nPos);
    bQuoted:=(sParams[nPos] = '"');
    if bQuoted then
      begin
        Inc(nPos);
        while (nPos <= Length(sParams)) and (sParams[nPos] <> '"') do
          begin
            sCP:=sCP+sParams[nPos];
            Inc(nPos);
          end;
      end
    else
      while (nPos <= Length(sParams)) and (sParams[nPos] <> ' ') do
        begin
          sCP:=sCP+sParams[nPos];
          Inc(nPos);
        end;
    if (EvalSet(Trim(sCP),sCP) = '') then
      exit;
    while (EvalSet(Pos(';',sCP),nPos) <> 0) do
      begin
        stl.Add(Copy(sCP,1,nPos-1));
        sCP:=Copy(sCP,nPos+1,Length(sCP)-nPos);
      end;
    stl.Add(sCP);
    if (sIJJar <> '') then
      for nPos:=0 to (stl.Count-1) do
        if (PosNc(sIJJar,stl[nPos]) <> 0) then
          begin
            Result:=nPos;
            break;
          end;
    if Assigned(stlCP) then
      stlCP.Assign(stl);
  finally
    stl.Free;
  end;
end;

procedure AdjustJVMParams(sIJJar,sIJDir: String; var sParams: String);
var
  stl: TStringList;
  sMem,sIJJarFile: String;
  nPos,nLen: Integer;
begin
  stl:=TStringList.Create;
  try
    // Verify/create the memory parameter
    sMem:=CreateMemParam(sParams);
    if (Pos(sMem,sParams) = 0) then
      sParams:=sMem+' '+sParams;
    // Locate the classpath param and adjust as needed
    if (ExtractClassPath(sIJJar,sParams,stl) = -1) or
       (PosNc(CLS_IMGJ,sParams) = 0) then
      begin
        if (stl.Count = 0) then // No classpath, insert after mem setting
          begin
            if (EvalSet(PosNc(CLP_MEM,sParams),nPos) <> 0) then
              while (nPos <= Length(sParams)) and (sParams[nPos] <> ' ') do
                Inc(nPos);
            if (nPos > Length(sParams)) then
              begin
                sParams:=sParams+' ';
                Inc(nPos);
              end;
            Inc(nPos);
            Insert(CLP_CLPTH+'  ',sParams,nPos);
          end
        else // Remove old classpath
          begin
            nPos:=PosNc(CLP_CLPTH,sParams)+Length(CLP_CLPTH)+1;
            nLen:=Pos(stl[stl.Count-1],sParams)+Length(stl[stl.Count-1]);
            if (sParams[nLen] = '"') then
              Inc(nLen);
            Delete(sParams,nPos,nLen-nPos);
          end;
        sIJJarFile:=ExtractFileName(sIJJar);
        for nPos:=0 to (stl.Count-1) do
          if SameText(sIJJarFile,ExtractFileName(stl[nPos])) then
            begin
              if (ExtractFilePath(stl[nPos]) = '') or not(FileExists(stl[nPos])) then
                stl[nPos]:=sIJJar;
              sIJJarFile:=''; // We found it or added it
              break;
            end
          else // Try expanding JAR file if needed
            if not(FileExists(stl[nPos])) and
               FileExists(sIJDir+ExtractFileName(stl[nPos])) then
              stl[nPos]:=sIJDir+ExtractFileName(stl[nPos]);
        // Only add sIJJar if it is NOT the default - allow user defined ij.jar files
        if (sIJJarFile <> '') and
           not(SameText(sIJJar,GetShortPathString(sIJDir+JAR_IMAGEJ)))then
          stl.Add(sIJJar);
        nPos:=PosNc(CLP_CLPTH,sParams)+Length(CLP_CLPTH)+1;
        // Sanity check
        if (PosNc(CLS_IMGJ,sParams) = 0) then
          Insert(CreateClassPath(sIJJar,stl)+' '+CLS_IMGJ,sParams,nPos)
        else
          Insert(CreateClassPath(sIJJar,stl),sParams,nPos);
      end;
    // Locate the ijpath param and add as needed
    if (PosNc(CLP_IJPATH,sParams) = 0) then
      begin
        nPos:=PosNc(CLS_IMGJ,sParams)+Length(CLS_IMGJ);
        Insert(Format(' %s %s',[CLP_IJPATH,GetShortPathString(sIJDir)]),sParams,nPos);
      end;
  finally
    stl.Free;
  end;
end;

function CreateCfgFile(sDefCfg,sDefDir,sDefJVM,sDefParams,sDefJar: String;
  bPrompt: Boolean): String;
var
  fCfg: Text;
  stl: TStringList;
  bCleanup: Boolean;
  sJTools: String;
  nPos,i: Integer;
begin
  bCleanup:=false;
  if (Trim(sDefCfg) = '') then
    Result:=ChangeFileExt(ParamStr(0),CFG_EXT)
  else
    Result:=sDefCfg;
  if bPrompt then
    MessageBox(0,'The program will now be auto-configured. You may be prompted '+
               'to input the locations of various files that can not be auto-detected. '+
               'Please consult the installation instructions for further assistance.',
               PChar('Welcome to '+TXT_APP),MB_OK or MB_ICONINFORMATION);
  try
    assignFile(fCfg,Result);
    rewrite(fCfg);
    bCleanup:=true;
    // Line 1
    if GetValidIJJar(sDefDir,sDefJar,true) then // Returns '.' if possible
      begin
        writeln(fCfg,sDefDir);
        if (sDefDir = '.') then // JAR is in CFG path
          sDefJar:=ExtractFileName(sDefJar);
      end
    else
      Abort;
    // Line 2
    if GetValidJVM(sDefDir,sDefJVM) then
      writeln(fCfg,RelativeJVM(sDefDir,sDefJVM)) // Save as relative path
    else
      Abort;
    // Line 3
    stl:=TStringList.Create;
    try
      sDefParams:=Trim(sDefParams);
      // Check for JAR and populate classpath list
      if (ExtractClassPath(sDefJar,sDefParams,stl) <> -1) then
        sDefJar:='';
      // Always check for tools.jar - if we need it, use it
      sJTools:=ExtractFilePath(sDefJVM); // Get folder
      Delete(sJTools,Length(sJTools),1); // Remove trailing '\'
      sJTools:=ExtractFilePath(sJTools)+DIR_TOOLS;
      if FileExists(sJTools) then
        begin
          sJTools:=GetShortPathString(sJTools);
          for i:=0 to (stl.Count-1) do // Check for existing tools.jar
            if SameText(JAR_TOOLS,ExtractFileName(stl[i])) then
              begin
                sJTools:='';
                break;
              end;
          if (sJTools <> '') then
            stl.Insert(0,sJTools);
        end;
      if (sDefJar <> '') then
        stl.Add(sDefJar);
      // Generate new params - preserve user memory settings if they exist
      sDefParams:=Format('%s %s %s %s',
        [CreateMemParam(sDefParams),CLP_CLPTH,CreateClassPath(sDefJar,stl),CLS_IMGJ]);
      writeln(fCfg,sDefParams);
    finally
      stl.Free;
    end;
    closeFile(fCfg);
    // Notify the user when a configuration file has been create
    MessageBox(0,PChar('A configuration file was successfully created as:'+#13#13+
               Result+#13#13+'with the following parameters:'+#13#13+
              'Line 1-  '+sDefDir+#13+
              'Line 2-  '+RelativeJVM(sDefDir,sDefJVM)+#13+
              'Line 3-  '+sDefParams+#13#13+
              'Please consult the installation instructions for further details.'),
              PChar(TXT_APP+' Configuration'),MB_OK or MB_ICONINFORMATION);
  except
    if bCleanup then
      begin
        closeFile(fCfg);
        DeleteFile(Result); // Cleanup
      end;
    FatalError('Unable to create the configuration file:'+#13#13+Result+#13#13+
               'Possible reasons include:'+#13+
               '  - missing or invalid '+TXT_APP+' home folder'+#13+
               '  - missing or invalid JAR file (default: '+JAR_IMAGEJ+')'+#13+
               '  - missing or invalid JVM (default: '+EXE_JVM+')'+#13+
               '  - write-access not allowed for configuration file'+#13#13+
               'Please check the installation parameters and try again.',
               'Configuration File Error',1);
  end;
end;

function ReadCfgFile(sCfgFile: String; var sGetDir,sGetJVM,sGetParams: String): Boolean;
var
  fCfg: Text;
  bCleanup: Boolean;
begin
  Result:=false;
  bCleanup:=false;
  // Read the config file for info - load each string then trim whitespace chars
  try
    assignFile(fCfg,sCfgFile);
    reset(fCfg);
    bCleanup:=true;
    readln(fCfg,sGetDir);
    sGetDir:=Trim(sGetDir);
    if (sGetDir = '') or (sGetDir = '.') then
      sGetDir:=ExtractFilePath(ParamStr(0));
    readln(fCfg,sGetJVM);
    sGetJVM:=Trim(sGetJVM);
    readln(fCfg,sGetParams);
    sGetParams:=Trim(sGetParams);
    if (sGetParams = '') then // Missing JVM or Parameters?
      sGetParams:=CLS_IMGJ;
    closeFile(fCfg);
    Result:=true; // All OK
  except
    if bCleanup then
      closeFile(fCfg);
  end;
end;

// -----------------------------------------------------------------------

function EvalSet(bEval: Boolean; var bResult: Boolean): Boolean;
begin
  bResult:=bEval;
  Result:=bEval;
end;

function EvalSet(sEval: String; var sResult: String): String;
begin
  sResult:=sEval;
  Result:=sEval;
end;

function EvalSet(nEval: Integer; var nResult: Integer): Integer;
begin
  nResult:=nEval;
  Result:=nEval;
end;

function GetShortPathString(sLongPathName: String): String;
var
  nLen: Integer;
begin
  Result:=sLongPathName;
  if (EvalSet(GetShortPathName(PChar(sLongPathName),nil,0),nLen) > 0) then
    begin
      SetLength(Result,nLen-1);
      GetShortPathName(PChar(sLongPathName),PChar(Result),nLen);
    end;
end;

function FreePIDL(var pidl: PItemIdList): Boolean;
var
  pMalloc: IMalloc;
begin
  pMalloc:=nil;
  if Assigned(pidl) then
    begin
      // Not sure if DidAlloc code is really necessary?  2004-02-01
      if Succeeded(SHGetMalloc(pMalloc)) then  // and (pMalloc.DidAlloc(pidl) > 0) then
        begin
          pMalloc.Free(pidl);
          pidl:=nil;
          Result:=true;
        end
      else
        Result:=false;
    end
  else
    Result:=true;
end;

function GetSpecialFolderPath(nFolder: Integer; bTrailingSlash: Boolean = true): String;
var
  pidl: PItemIDList;
  szPath: Array[0..MAX_PATH] of Char;
begin
  Result:='';
  pidl:=nil;
  FillChar(szPath,SizeOf(szPath),#0);
  if Succeeded(SHGetSpecialFolderLocation(0,nFolder,pidl)) then
    begin
      SHGetPathFromIDList(pidl,szPath);
      FreePIDL(pidl);
    end;
  Result:=Trim(szPath);
  if bTrailingSlash and (Result <> '') and (Result[Length(Result)] <> '\') then
    Result:=Result+'\';
end;

// Get parameters passed on the commandline as single string
function GetCmdLnParams: String;
var
  nPos: Integer;
begin
  if (ParamCount = 0) then
    Result:=''
  else
    begin
      Result:=GetCommandLine;
      // Skip the first param which is the executable
       if (Result[1] = '"') then // Quoted?
         nPos:=Pos('"',Copy(Result,2,Length(Result)-1))+1 // Skip quote
       else
         nPos:=Pos(' ',Result);
      Result:=Trim(Copy(Result,nPos+1,Length(Result)-nPos));
    end;
end;

function NormalizeDir(sPath: String; bTrailingSlash: Boolean = true): String;
var
  sRoot: String;
  nPos: Integer;
begin
  Result:=Trim(sPath);
  if (Result <> '') then
    begin
      while (Pos('/',Result) <> 0) do
        Result[Pos('/',Result)]:='\';
      // This prevents path from ending in multi-"\"
      while (Result <> '') and (Result[Length(Result)] = '\') do
        Delete(Result,Length(Result),1);
      if bTrailingSlash then
        Result:=Result+'\';
      while (EvalSet(Pos('\..',Result),nPos) <> 0) and
            (Pos('\',EvalSet(Copy(Result,1,nPos-1),sRoot)) <> 0) do
        begin
          while (sRoot <> '') and (sRoot[Length(sRoot)] <> '\') do
            Delete(sRoot,Length(sRoot),1);
          Result:=sRoot+Copy(Result,nPos+4,Length(Result)-nPos); // 4 = Length('\..')+1
        end;
    end;
end;

function PosNc(sSubStr,sStr: String): Integer;
begin
  Result:=Pos(UpperCase(sSubStr),UpperCase(sStr));
end;

INITIALIZATION
  CoInitialize(nil);
FINALIZATION
  CoUninitialize;
END.
