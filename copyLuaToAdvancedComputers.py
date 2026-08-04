import os
import shutil

# copy AcLuaDev/lua/AdvancedOS/uefi.lua --> MCAdvancedComputers/src/main/resources/assets/advancedcomputers/lua/uefi.lua
# copy AcLuaDev/lua/AdvancedOS/disk1/* --> MCAdvancedComputers/src/main/resources/assets/advancedcomputers/lua/premade_floppies/acos/* (except boot.cfg, etc/fstab)
acLuaDevPath = os.path.dirname(os.path.realpath(__file__))

assert os.path.isfile(os.path.join(acLuaDevPath, "copyLuaToAdvancedComputers.py")), "somehow detecting the acluadev folder failed"  # sanity check
print("AcLuaDev is at: " + acLuaDevPath)

acPath = os.path.abspath(os.path.join(acLuaDevPath, "../MCAdvancedComputers/"))
print("AC is at: " + acPath)


shutil.copy(os.path.join(acLuaDevPath, "lua/AdvancedOS/uefi.lua"), os.path.join(acPath, "src/main/resources/assets/advancedcomputers/lua/uefi.lua"))



osDestPath = os.path.abspath(os.path.join(acPath, "src/main/resources/assets/advancedcomputers/lua/premade_floppies/acos"))
filesNotToDeleteOrCopy = ["boot.cfg", "etc/fstab"]

# remove all unblacklisted files

print("Removing old files...")
for (root, dirs, files) in os.walk(osDestPath, topdown=False):
    for dir in dirs:
        dirPath = os.path.abspath(os.path.join(root,dir))
        #print(f"checking if {dirPath} is empty")
        if len(os.listdir(dirPath)) == 0:
            assert dirPath.startswith(osDestPath)
            os.rmdir(dirPath)

    for f in files:
        filePath = os.path.abspath(os.path.join(root,f))
        relativePath = os.path.relpath(filePath, osDestPath).replace("\\","/")
        assert "../" not in relativePath, "relative path looks odd"
        assert filePath.startswith(osDestPath)
        if relativePath not in filesNotToDeleteOrCopy:
            os.remove(filePath)

luaSrcPath = os.path.join(acLuaDevPath, "lua/AdvancedOS/disk1")
print(f"Copying {luaSrcPath} --> {osDestPath}")
for (root, dirs, files) in os.walk(luaSrcPath, topdown=True):
    #print(f"SRC IS {root}")
    destRoot = os.path.abspath(os.path.join(osDestPath,os.path.relpath(root, luaSrcPath)))
    #print(f"DST IS {destRoot}")

    for dir in dirs:
        currDestPath = os.path.join(destRoot, dir)
        if not os.path.exists(currDestPath):
            os.mkdir(currDestPath)
    
    for file in files:
        currDestPath = os.path.join(destRoot, file)
        if not os.path.exists(currDestPath):
            shutil.copy(os.path.join(root, file), currDestPath)

print("Done!")