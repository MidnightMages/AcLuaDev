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
function string.split(str, delim) 
    assert(#delim == 1, "only delim len 1 supported for now")
    local rv = {}
    local buf = ""
    for i = 1, #str do
        local c = string.sub(str,i,i)
        if c == delim then
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
if bootDrive == nil then
    for t, a in component.list() do 
        if t == "disk" and a.fileExists("boot.lua") then bootDrive = a; break; end        
    end
end
assert(bootDrive ~= nil, "unable to rediscover bootdrive")

---@diagnostic disable-next-line: missing-fields
_G.package = {}
package.path = "/lib/?.lua"
package.loaded = {}
package.loaded.filesystem = assert(load(bootDrive.open("/lib/filesystem.lua").read())(), "failed to initialize filesystem")
local fs = package.loaded.filesystem -- fs = require("filesystem")


function require(moduleName)
    assert(moduleName and #moduleName > 0, "module name must be a nonempty string")
    assert(#string.split(moduleName,"/"), "module name cannot contain slashes")

    local existing = package.loaded[moduleName]
    if existing ~= nil then return existing end
    for _, p in ipairs(string.split(package.path,";")) do
        local path = string.replace(p, "?", moduleName)
        if fs.isFile(path) then
            local code = fs.open(path).read()
            local rv = load(code, moduleName, "t", _ENV)
            package.loaded[moduleName] = rv
            return rv
        end
    end
    error("module '"..tostring(moduleName).."' could not be found in package.path")
end

fs = require("filesystem") -- to keep the lua plugin happy
fs:init(bootDrive)
print(fs:readAllText("/lib/filesystem.lua"))

pp(_G)

--sleep(5)
xpcall = function (...)
    return ...
end

local stringBuffer = ""
local function keyTyped(key) -- return whether to exit
    if key == "\b" then
        if #stringBuffer > 0 then
            printInline(key)
            stringBuffer = stringBuffer:sub(1, #stringBuffer - 1)
        end
    else
        printInline(key)
    end

    if key == "\n" then
        if stringBuffer == "exit()" then
            return true
        end
        local res, err = load(stringBuffer, "", "t", _G)
        stringBuffer = ""
        if not res then
            print("Error: ", err)
        else
            local rvs = table.pack(pcall(res))
            --[[
            local rvs = table.pack(xpcall(res,function(msg)
                local trcb = debug.traceback("X-ERR: " .. tostring(msg), 2)
                for i = 1, 4, 1 do
                    trcb = trcb:sub(1, trcb:match("^.*()\n") - 1)
                end
                print(trcb)
            end))]]
            rvs[1] = rvs[1] and "OK" or "ERROR"
            --if #rvs > 1 then
            print(table.unpack(rvs))
            --end
        end
        printInline(">> ")
    elseif key ~= "\b" then
        stringBuffer = stringBuffer .. key
    end
end

printInline(">> ")
while true do
    local event, a1, a2 = computer.getMachineEvent()
    if event == nil then
        sleep(0.05)
    elseif event == "shutdown" then
        break
    elseif event == "keyTyped" then
        if keyTyped(a1) then break end
    end
end

-- init filesystem
-- init shell
-- run autorun.lua files