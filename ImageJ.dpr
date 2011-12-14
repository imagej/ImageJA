PROGRAM ImageJ;

//
//  Author: George H. Silva <George.Silva@chemie.bio.uni-giessen.de>
//  Version History:
//    v1.0.0.x - 2006/1/21 - initial release
//             - Basic code based on "The Java launcher for Windows v1.22" by Jacob Marner
//             - Improved/customized for ImageJ (i.e. can handle file-associations, etc)
//             - This windows stub can be used for other Java applications as well;
//               just change the icon, set the DEF_CFG constant and re-compile!
//
//    v1.1.0.x - 2006/1/23 - many updates/improvements
//             - Robust loader to handle various config scenarios
//             - Code to auto-create the config file
//             - Code for auto-detection of various parameters (JVM, mem usage, etc)
//             - Code to allow selection of needed files
//             - This version essentially can reconfigure itself as needed, depending
//               on how the user changes their JAVA install
//
//    v1.2.0.x - 2006/1/24 - minor updates/improvements
//             - Bug fixes and logic changes per Wayne's suggestions
//
//    v1.3.0.x - 2006/1/24 - minor updates/improvements
//             - Correctly expands command-line params to full-paths as needed
//             - Preserves relative path for JRE if possible
//             - No longer needlessly expands relative paths and re-saves CFG file
//             - Improved logic to make sure tools.jar is in JVM params
//
//    v1.4.0.x - 2006/1/25 - major updates/improvements
//             - Undo command-line expansion bug
//             - Supports all forms of ImageJ execution - all paths, params, etc
//             - Improved logic for adjusting JVM parameters
//
//    v1.4.1.x - 2006/1/31 - minor updates/improvements
//             - New default for setting memory parameter
//             - Additional feedback added for failure situations
//
//    v1.4.2.x - 2006/2/1 - minor updates/improvements
//             - Fixed OpenDilaog code to work with older Win9x systems
//
//    v1.4.3.x - 2006/2/1 - Changed File Description to: "ImageJ Launcher"
//

USES
  Windows, SysUtils, ShellAPI, Shared;

{$R *.res}

VAR
  nSaveCfg: Integer;
  sCfg,sIJDir,sJVM,sParams,
  sIJJar,sJVMEx,sParamsEx: String;

BEGIN
  // Read/Write mode for files (global)
  FileMode:=2;
  nSaveCfg:=0;
  // Look for the config file - default to DEF_CFG or create one as needed
  if not(FileExists(Evalset(ChangeFileExt(ParamStr(0),CFG_EXT),sCfg))) then
    begin
      if SameText(ExtractFileName(sCfg),DEF_CFG) then
        sCfg:=CreateCfgFile('','','','','',true) // Create a new CFG file
      else // Try default CFG
        if not(FileExists(EvalSet(ExtractFilePath(ParamStr(0))+DEF_CFG,sCfg))) then
          sCfg:=CreateCfgFile('','','','','',true) // Create a new CFG file
    end;
  // Read config and auto-repair if needed
  if not(ReadCfgFile(sCfg,sIJDir,sJVM,sParams)) then
    begin
      if (MessageBox(0,PChar('The configuration file is corrupt. Would you like to '+
                     'auto-repair this file?'),PChar('Bad Configuration File Format'),
                     MB_YESNO or MB_ICONQUESTION) = ID_NO) then
        FatalError('The configuration file should contain at least 3 lines:'+#13#13+
                   'Line 1-  The '+TXT_APP+' working directory ('+CLP_IJPATH+')'+#13+
                   'Line 2-  The location of the java virtual machine (JVM) executable'+#13+
                   'Line 3-  Parameters to pass to the JVM'+#13#13+
                   'To restore the default configuration, simply re-start '+TXT_APP+' after '+
                   'deleting this file:'+#13#13+sCfg,
                   'Bad Configuration File Format',2)
      else
        begin
          sCfg:=CreateCfgFile(sCfg,'','','','',false);
          if not(ReadCfgFile(sCfg,sIJDir,sJVM,sParams)) then
            FatalError('The configuration file could not be repaired. Please check the '+
                       TXT_APP+' installation and try again','Bad Configuration File Format',3)
        end;
    end;
  // Track if user wants a new JAR file
  sIJJar:=GetShortPathString(sIJDir+JAR_IMAGEJ);
  // Validate the ImageJ folder - sIJDir should be expanded at this point
  if not(DirectoryExists(sIJDir)) then
    begin
      if GetValidIJJar(sIJDir,sIJJar,false) then
        Inc(nSaveCfg) // Save new data below if all is ok
      else
        FatalError('Could not locate the '+TXT_APP+' JAR file ('+JAR_IMAGEJ+'). Please '+
                   'check the installation and try again.','Fatal Error',4);
    end;
  // Validate the JVM; at this point:
  //  sJVM  = relative OR full path to JVM
  //  sJVMEx = actual full path to JVM
  sJVMEx:=sJVM;
  if not(FileExists(sJVMEx)) then
    begin
      // Save new data below if all is ok
      if GetValidJVM(sIJDir,sJVMEx) then // This will expand sJVMEx as needed
        begin
          // Verify it was not a relative path
          if not(SameText(sJVM,RelativeJVM(sIJDir,sJVMEx))) then
            begin
              sJVM:=sJVMEx;  // New path
              Inc(nSaveCfg); // Save data below if all is ok
            end;
        end
      else
        FatalError('Could not locate the JVM file ('+EXE_JVM+'). Please '+
                   'check the installation and try again.','Fatal Error',5);
    end;
  // Get any passed parameters - do this before validation
  sParamsEx:=Trim(sParams+' '+GetCmdLnParams);
  // Adjust the parameters - expands JAR paths and adds ImageJ path
  AdjustJVMParams(sIJJar,sIJDir,sParamsEx);
  // Check if we have a new JAR that should be saved to CFG file
  if (ExtractClassPath(sIJJar,sParamsEx,nil) = -1) then
    Inc(nSaveCfg);
  // Run the program
  if (ShellExecute(0,nil,PChar(sJVMEx),PChar(sParamsEx),
                   PChar(GetCurrentDir),SW_SHOWNORMAL) <=32) then
    FatalError('Could not launch '+TXT_APP+' as configured. Please check the '+
               'settings and try again.','Fatal Error',6)
  else
    if (nSaveCfg > 0) then // Save this if needed
      CreateCfgFile(sCfg,sIJDir,sJVM,sParams,sIJJar,false);
END.

