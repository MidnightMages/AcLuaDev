--local a = {}
--setmetatable(a,a)
--a[1] = 1
function _G.pp(tbl)
    for k,v in pairs(tbl) do
        print(k, ":", v)
    end
end
       
local ud = bootDrive
print("------------------KEYS------------------")
pp(vm.listUDKeys(ud))
print("----------------------------------------")
print(type(ud), typeof(ud))

_ENV._EXT.string = _ENV.string
function string.endsWith(str, suffix) return string.sub(str, #str-#suffix+1) == suffix end
function string.trimRight(str, toTrim)
    assert(#toTrim == 1, "toTrim must be exactly of length 1")
    local lastLetterToTrim = #str
    while lastLetterToTrim >= 1 do
        if str:sub(lastLetterToTrim,lastLetterToTrim) ~= toTrim then
            break
        else
            lastLetterToTrim = lastLetterToTrim - 1
        end
    end
    return str:sub(1,lastLetterToTrim)
end

function string.startsWith(str, prefix) return string.sub(str, 1, #prefix) == prefix end
---@param delim string
---@param ... string
function string.join(delim, ...) return table.concat(table.pack(...), delim) end
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
function string.charCount(str, charToCount)
    local rv = 0
    assert(#charToCount == 1, "charToCount must be exactly of length 1")
    for i = 1, #str do
        if str:sub(i,i) == "charToCount" then
            rv = rv + 1
        end
    end
    return rv
end

--print("this is some text")
--print(string.replace("this is some text","i","IJK"))

--print(string.startsWith("testbla","ttes"), "<-- test")



local bootDrive = _G.bootDrive
_G.components = {}

for k,v in component:list() do
    components[k] = v
end
if bootDrive == nil then
    for t, a in pairs(components) do 
        if t == "massStorage" and a.fileExists("boot.lua") then bootDrive = a; break; end        
    end
end
assert(bootDrive ~= nil, "unable to rediscover bootdrive")

---@diagnostic disable-next-line: missing-fields
_G.package = {}
package.path = "/lib/?.lua"
package.loaded = {}
-- init filesystem
package.loaded.filesystem = assert(load(bootDrive:open("/lib/filesystem.lua"):read(), "/lib/filesystem.lua")(), "failed to initialize filesystem")
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

-- test code
--[[
local main, isMain = coroutine.running()
assert(isMain,"1")
local co = coroutine.create(function(a)
    print("yielding")
    local co2, isMain2 = coroutine.running()
    assert(not isMain2)
    local co3 = coroutine.create(function (b)
        print("yielding inner")
        local co3, isMain3 = coroutine.running()
        assert(not isMain3)
        coroutine.yield()
        print("inner coroutine has run")
    end)
    coroutine.yield()
    print("running inner")
    coroutine.resume(co3)
    print("running inner again")
    coroutine.resume(co3)
    print("co has run")
end)
coroutine.resume(co)
coroutine.resume(co)

sleep(5)
]]--[[
local volflag = 0
function f(arg)
    volflag = volflag + 1
    coroutine.yield()
    print(arg, volflag)
end

local co = coroutine.create(function()
    f("b")
end)
local co2 = coroutine.create(function()
   f("a")
    coroutine.resume(co)
    coroutine.resume(co)
end)

coroutine.resume(co2)
coroutine.resume(co2)
sleep(5)]]
-- end test code

print("Loading kernel...")
local kernel = require("kernel")

-- init shell
--kernel:startProcessFromPath("/bin/lua.lua")
kernel:startProcessFromPath("/bin/sh.lua")

print("Starting kernel...")
kernel:run()

-- run autorun.lua files