---@diagnostic disable: duplicate-set-field
--[[
Default BOOTLOADER implementation of AdvancedComputers
This is the entry point of execution of AdvancedOS

Expected setup from UEFI:
- print
- printInline
- shutdown
- bootDrive -- TODO take as ARGUMENT
]]


-- kernel utils table
kutils = {}


-- Bootstrap the file system and initialize "require"
print("setting up require and loaddriver ...")

---@diagnostic disable-next-line: missing-fields
_ENV.package = {}
package.path = "/lib/?.lua"
package.loaded = {}

local bootDrive = _ENV.bootDrive
assert(bootDrive, "BOOTLOADER: undefined boot drive")

-- init filesystem
local fsHandle = bootDrive:open("/sys/filesystem.lua")
package.loaded.filesystem = assert(
    load(fsHandle:read(-1),
        "/sys/filesystem.lua")(),
    "failed to initialize filesystem")
fsHandle:close()
local fs = package.loaded.filesystem -- corresponds to fs = require("filesystem")

function loadfile(path)
    local c = fs:readAllText(path)
    return assert(load(c, path, "t", _ENV))
end

function dofile(path, ...)
    return loadfile(path)(...)
end


print("setting up string helpers ...")
dofile("/sys/stringhelpers.lua")




-- require should be a user thing, the kernel should not use that
function require(moduleName, privileged)
    assert(moduleName and #moduleName > 0, "module name must be a nonempty string")
    assert(#string.split(moduleName, "/"), "module name cannot contain slashes")

    print()

    local rv = nil
    local existing = package.loaded[moduleName]
    if existing ~= nil then return existing end
    for _, p in ipairs(string.split(package.path, ";")) do
        local path = string.replace(p, "?", moduleName)
        assert(privileged or not path:startsWith("/sys/"))
        if fs:fileExists(path) then
            rv = dofile(path)
            package.loaded[moduleName] = rv
            break
        end
    end
    if rv then return rv end
    error("module '" .. tostring(moduleName) .. "' could not be found in package.path")
end
fs:init(bootDrive)
-- filesystem and require done


print("initializing kernel utils ...")
-- component wrapping helpers
local wrappingComponentKey = {}
local metaPrototypes = {}
local noProtoMeta = {
    __metatable = false,
    __pairs = false,
    __ipairs = false,
    __newindex = function (t, k, v)
        error("setting properties for components is not allowed")
    end
}

function kutils.wrapComponent(comp, prototype)
    local meta
    if prototype == nil then
        meta = noProtoMeta
    elseif metaPrototypes[prototype] ~= nil then
        meta = metaPrototypes[prototype]
    else 
        meta = {
            __metatable = false,
            __pairs = false,
            __ipairs = false,
            __index = prototype,
            __newindex = function (t, k, v)
                error("setting properties for components is not allowed")
            end
        }
        metaPrototypes[prototype] = meta
    end
    return setmetatable({[wrappingComponentKey] = comp}, meta)
end

function kutils.unwrapComponent(wrapped, reqType)
    local comp = rawget(wrapped, wrappingComponentKey)
    if reqType ~= nil and comp.componentType ~= reqType then
        error("incorrect driver selected for component")
    end
    return comp
end

function kutils.registerPermission(name)
    -- TODO
end

function kutils.assertPermission(name)
    -- TODO
end

function kutils.assertType(obj, tname)
    if type(obj) ~= tname then
        error("type error: expected "..tname..", got "..type(obj))
    end
    return obj
end




print("setting up proccess handling ...")
local PROCESS <const> = {
    curId = 0
}

function PROCESS.new()
    local id = PROCESS.curId
    PROCESS.curId = id + 1
    return setmetatable({
        id = id,
        pausedUntil = -1,
        handlers = {},
        mainThread = nil,
        curThread = nil,
        blockedThreads = {},
    }, {
        __index = PROCESS
    })
end



--[[
user YIELD engineering thoughts:
 - could be a syscall
 - could return to outer coroutine
 - could wait for time / hwevent / other process unblock (basically thread.join for processes or wait/notifyAll)
 - MUST check if it may actually yield (not possible if user code is run in kernel/driver context (e.g. unblock or other predicate))
]]

--[[
user RESUME engineering thoughts:
 - is actually a true yield with return to thread loop blocking THIS tread and unblocking resuming thread
 - MUST check if possible (i.e. outside of kernel/driver)
]]

--[[
scheduler:
 - version 0.1 will be a simple round robin event first proc scheduler
 - event queue which distributes events to all subscribed processes
]]

--[[
primitives:
 - require
 - stdio
 - process communication (pipes, in, out)
]]

--[[
users:
 - permissions per user
 - processes per user
 - init process for user (i.e. one shell that kills the user if closed)
 - /etc/passwd
]]





print("setting up kernel infrastructure ...")
local syscalls = {}
function panic(msg)
    -- TODO
end
function doSyscall()
    -- TODO
end








-- maybe install OS or run live system

local bootCfg = { -- default options
    showLiveSystemMenu = false
}

if fs:fileExists("/boot.cfg") then
    local bootCfgText = string.normalizeLineEndings(fs:readAllText("/boot.cfg"))
    local bootCfgLines = string.split(bootCfgText, "\n")
    for i = 1, #bootCfgLines do
        local splitted = string.split(bootCfgLines[i], "=")
        local currV = bootCfg[splitted[1]]
        local newVStr = splitted[2]
        if currV ~= nil then -- if option exists
            local currVType = type(currV)
            if currVType == "boolean" then
                if newVStr == "true" or newVStr == "false" then
                    bootCfg[splitted[1]] = newVStr == "true"
                    goto continue
                end
            else
                error("Boot cfg type of " .. tostring(splitted[1]) .. " (" .. currVType .. ") is not defined.")
            end
            error("Boot option " ..
                tostring(splitted[1]) .. " was given an invalid value for expected type " .. tostring(currVType) .. "!")
        end
        error("Boot option " .. tostring(splitted[1]) .. " does not exist!")

        ::continue::
    end
end

local function readPrimitiveInput()
    local readInput = ""
    while true do
        local nextEvent = table.pack(components:getFirst("computer"):getMachineEvent())
        if nextEvent[1] == nil then
            sleep(0.1)
        elseif nextEvent[1] == "keyTyped" then
            local chr = nextEvent[2]
            if chr == "\n" then
                print()
                return readInput
            end
            if chr == "\b" then
                if #readInput > 0 then
                    readInput = string.sub(readInput, 1, -2)
                    printInline(chr)
                end
            else
                printInline(chr)
                readInput = readInput .. chr
            end
        elseif nextEvent[1] == "shutdown" then
            error("shutdown requested")
        end
    end
end

local function selectIntegerOption(title)
    print(title)
    return assert(tonumber(readPrimitiveInput()), "invalid option given")
end

local function showHeading(text, spacerChar)
    spacerChar = spacerChar or "="
    local spacer = string.rep("=", #text + 4)
    local textPad = string.rep(" ", 2)
    print(spacer .. "\n" .. textPad .. text .. "\n" .. spacer)
end
if bootCfg.showLiveSystemMenu then
    showHeading("INSTALL-MEDIUM BOOT MENU")
    local option = selectIntegerOption(
        "Select an option by typing the corresponding number and pressing ENTER:\n 1) Install\n 2) Boot from this medium directly")
    if option == 1 then
        showHeading("DESTINATION DISK SELECTION")
        local destMntPath = "/mnt/"
        local suffix = ""
        local nextId = 1
        local availableDisks = {}
        for t, a in components:list() do
            if a.componentType == "massStorage" then
                local desc = a.storageFamilyName .. "-" .. a.storageApiType .. "-" .. tostring(a.diskId)
                local hasOs = a:fileExists("boot.lua")
                local attribs = {}
                if a == bootDrive then table.insert(attribs, "BOOTED FROM") end
                table.insert(attribs, hasOs and "HAS OS" or "NO OS")

                desc = desc .. " [" .. table.concat(attribs, ", ") .. "]"

                local isAllowed = a.storageApiType == "managed" and a ~= bootDrive
                suffix = suffix .. "\n " .. (isAllowed and tostring(nextId) or "-") .. ") " .. desc
                if isAllowed then
                    availableDisks[nextId] = { a, hasOs }
                    nextId = nextId + 1
                end
            end
        end

        local diskOption = selectIntegerOption("Found disks are listed below. Select one to install to:\n" .. suffix)

        print("Mounting...")
        fs:addMountPoint(destMntPath, availableDisks[diskOption][1])
        -- TODO clear the target filesystem before writing
        print("Copying files...")
        local blacklist = { destMntPath, "/boot.cfg" }
        fs:copyRecursive("/", destMntPath, blacklist, true)
        print("Installation complete. Press enter to exit.")
        readPrimitiveInput()
        return
    elseif option == 2 then
        -- continue
    else
        error("received out of range option: " .. tostring(option))
    end
end

-- live system stuff done





-- init

print("Loading kernel...")
local kernel = require("kernel")

-- init shell
-- kernel:startProcessFromPath("/bin/lua.lua")
kernel:startProcessFromPath("/bin/sh.lua")

print("Starting kernel...")
kernel:run()

-- run autorun.lua files
