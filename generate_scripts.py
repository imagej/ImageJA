import imagej
import scyjava as sj
import code

def get_command_table():
    """
    Return Menu table from ImageJ.
    """
    command_table = Menus.getCommands()
    return command_table

def hashtable_to_dict(hashtable):
    """
    Convert java.util.Hashtable to python dict.
    :param hashtable: Command hashtable from ImageJ.
    """
    command_dict = {}
    
    for command in hashtable:
        command_dict[str(command)] = str(hashtable.get(command))

    return command_dict

def run_commands(cmd_dict:dict):
    """
    Run all commands in the dictionary.
    :param cmd_dict: Python dictionary of the command table.
    """
    arg = ""
    cmd_count = 0

    for k, v in cmd_dict.items():
        cmd_count += 1
        if v.endswith("\")"):
            arg_start = v.rfind("(\"")
            if arg_start > 0:
                arg = v[arg_start + 2:len(v) - 2]
                v = v[:arg_start]
            
        try:
            print(f"Executing: {k}")
            IJ.runPlugIn(k, v, arg)
        except:
            print(f"Failed: {k} : {v}")

    print(f"Count: {cmd_count}")

    return

if __name__ == "__main__":
    # initiaize imagej
    print("Starting ImageJ...")
    ij = imagej.init(headless=False)
    print(f"ImageJ Version: {ij.getVersion()}")

    # load classes
    IJ = sj.jimport('ij.IJ')
    Menus = sj.jimport('ij.Menus')

    # get commands
    table = get_command_table()
    cmd_dict = hashtable_to_dict(table)
    run_commands(cmd_dict)

    # return interperter
    code.interact(local=locals())