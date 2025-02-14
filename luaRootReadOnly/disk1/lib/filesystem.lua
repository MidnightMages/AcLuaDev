local fs = {}
-- TODO create file containing fs metadata, listing ids of disks and mountpoints, 
-- such that it is only necessary for the boot drive to be in a predictable slot or specified by the bios, but the others can be in any order
local mounts = {}

local function normalizePath(path)
    -- TODO add process's working directory if the path odes not start with /
    local segments = string.split(path,"/")
    --print("splitres:",#segments, segments[1]..";", segments[2]..";")
    local skipCnt = 0
    local output = ""
    for i = #segments, 1, -1 do
        local seg = segments[i]
        print("seg:",seg)
        if seg == ".." then
            skipCnt = skipCnt + 1
        elseif seg ~= "." then
            if skipCnt > 0 then
                skipCnt = skipCnt - 1
            else
                if #output > 0 then
                    seg = seg.."/"
                end
                output = seg..output
            end
        end
    end
    return output
end

function fs:addMountPoint(path, drive)
    if not string.endsWith(path, "/") then
        path = path .. "/"
    end
    assert(drive, "no drive specified")
    assert(not mounts[path], "mount point '"..path.."' already exists")
    mounts[path] = drive
end

local function getMountPoint(path)
    local currPrefix = "/"
    local currDrive = mounts[currPrefix]
    local currLen = #currPrefix
    for prefix, drive in pairs(mounts) do
        if #prefix > currLen and string.startsWith(path, prefix) then
            currDrive, currPrefix = drive, prefix
        end
    end
    return currDrive, currPrefix
end

function fs:readAllText(filePath)
    assert(string.startsWith(filePath,"/"))
    local p = normalizePath(filePath)
    local drive, prefix = getMountPoint(p)
    local drivePath = string.sub(p, #prefix+1)
    print("a", drivePath, p, filePath)
    return drive.open(drivePath).read()
end

function fs:init(bootDrive)
    assert(bootDrive)
    fs:addMountPoint("/", bootDrive)
    -- load other mountpoints from fstab
    if true then 
    return nil end
    local t = fs:readAllText("/fstab")
    for _, v in ipairs(string.split(t,"\n")) do
        local s = string.split(v, "=", 2)
        assert(#s == 2, "invalid mountpoint definition: "..tostring(v))
        fs:addMountPoint(table.unpack(s))
    end
end

return fs