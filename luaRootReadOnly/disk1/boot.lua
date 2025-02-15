--local a = {}
--setmetatable(a,a)
--a[1] = 1
local function pp(tbl)
    for k,v in pairs(tbl) do
        print(k, ":", v)
    end
end


function string.endsWith(str, suffix) return string.sub(str, #str-#suffix+1) == suffix end
function string.startsWith(str, prefix) return string.sub(str, 1, #prefix) == prefix end
function string.split(str, delim, maxResultCountOrNil)
    assert(#delim == 1, "only delim len 1 supported for now")
    maxResultCountOrNil = (maxResultCountOrNil or 0)-1
    local rv = {}
    local buf = ""
    for i = 1, #str do
        local c = string.sub(str,i,i)
        if #rv ~= maxResultCountOrNil and c == delim then
            table.insert(rv, buf)
            buf = ""
        else
            buf = buf..c
        end
    end
    table.insert(rv, buf)
    return rv
end
function string.replace(str, search, replacement)
    local rv = ""
    local consumedLen = 1
    local i = 1
    while i<#str do
        if string.sub(str, i, i+#search-1) == search then
            rv = rv .. string.sub(str, consumedLen, i-1) .. replacement
            i=i+#search
            consumedLen = i
        end
        i=i+1
    end
    return rv .. string.sub(str, consumedLen)
end

--print("this is some text")
--print(string.replace("this is some text","i","IJK"))

--print(string.startsWith("testbla","ttes"), "<-- test")



local bootDrive = _G.bootDrive
_G.components = {}

for k,v in component.list() do
    components[k] = v
end
if bootDrive == nil then
    for t, a in pairs(components) do 
        if t == "disk" and a.fileExists("boot.lua") then bootDrive = a; break; end        
    end
end
assert(bootDrive ~= nil, "unable to rediscover bootdrive")

---@diagnostic disable-next-line: missing-fields
_G.package = {}
package.path = "/lib/?.lua"
package.loaded = {}
-- init filesystem
package.loaded.filesystem = assert(load(bootDrive.open("/lib/filesystem.lua").read())(), "failed to initialize filesystem")
local fs = package.loaded.filesystem -- fs = require("filesystem")


function loadfile(path)
    local c = fs:readAllText(path)
    return assert(load(c, path, "t", _ENV))
end
function dofile(path, ...) return loadfile(path)(...) end

function require(moduleName)
    assert(moduleName and #moduleName > 0, "module name must be a nonempty string")
    assert(#string.split(moduleName,"/"), "module name cannot contain slashes")

    local rv = nil
    local existing = package.loaded[moduleName]
    if existing ~= nil then return existing end
    for _, p in ipairs(string.split(package.path,";")) do
        local path = string.replace(p, "?", moduleName)
        if fs:fileExists(path) then
            rv = dofile(path)
            package.loaded[moduleName] = rv
            break
        end
    end
    if rv then return rv end
    error("module '"..tostring(moduleName).."' could not be found in package.path")
end

fs = require("filesystem") -- to keep the lua plugin happy

fs:init(bootDrive)
--print(fs:readAllText("/lib/filesystem.lua"))
--pp(_G)
--sleep(5)

print("Loading kernel...")
local kernel = require("kernel")

-- init shell
kernel:startProcess("/bin/lua.lua")

print("Starting kernel...")
kernel:run()

-- run autorun.lua files