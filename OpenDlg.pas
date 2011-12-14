UNIT OpenDlg;

// Simple File Open dialog - very lightweight compared to TOpenFile

INTERFACE

uses
  Windows;

function OpenDialog(hParent: HWnd; sTitle,sInitialDir, sFilter: String;
  var sFileName: String): Boolean;

IMPLEMENTATION

uses
  SysUtils, CommDlg, ShlObj;

function FixFilterStr(sFilter: String): String;
var
  i: Integer;
begin
  Result:=sFilter;
  for i:=1 to Length(sFilter) do
    if (Result[i] = '|') then
      Result[i]:=#0;
  Result:=Result+#0; // Double null terminators
end;

function OpenDialog(hParent: HWnd; sTitle,sInitialDir, sFilter: String;
  var sFileName: String): Boolean;
var
  ofn: TOpenFileName;
  szFile: Array[0..MAX_PATH] of Char;
begin
  Result:=false;
  FillChar(ofn,SizeOf(TOpenFileName),0);
  with ofn do
    begin
      // Code from Dialogs.pas to initialize Structure size variable
      if (Win32MajorVersion >= 5) and (Win32Platform = VER_PLATFORM_WIN32_NT) or // Win2k
         ((Win32Platform = VER_PLATFORM_WIN32_WINDOWS) and (Win32MajorVersion >= 4) and
          (Win32MinorVersion >= 90)) then // WinME
        lStructSize:=SizeOf(TOpenFilename)
      else // Subtract size of added fields
        lStructSize:=SizeOf(TOpenFilename)-(SizeOf(DWORD) shl 1)-SizeOf(Pointer);
      hInstance:=SysInit.HInstance;
      hwndOwner:=hParent;
      nMaxFile:=SizeOf(szFile);
      FillChar(szFile,nMaxFile,0);
      lpstrFile:=szFile;
      if (sTitle <> '') then
        lpstrTitle:=PChar(sTitle);
      if (sInitialDir = '') then
        lpstrInitialDir:='.'
      else
        lpstrInitialDir:=PChar(sInitialDir);
      StrLCopy(lpstrFile,PChar(sFileName),MAX_PATH);
      sFilter:=FixFilterStr(sFilter);
      lpstrFilter:=PChar(sFilter);
      Flags:=OFN_HIDEREADONLY or OFN_FILEMUSTEXIST or OFN_EXPLORER or OFN_ENABLESIZING;
    end;
  if GetOpenFileName(ofn) then
    begin
      sFileName:=StrPas(szFile);
      Result:=true;
    end;
end;

end.
