--[[

]]


local assertPermission = kutils.assertPermission
local wrap = kutils.wrapObjectWithAccess
local unwrap = kutils.unwrapObject

local DRIVER_NAME <const> = "process"
local PERMISSION <const> = "drv.process"
kutils.registerPermission(PERMISSION)


local PROCESS <const> = {
    curId = -1
}

local ALLOWED_READS = {
    id = true,
    description = true,
    currentWorkingDirectory = true,
    args = true,
}

---@class OsThread
---@field id integer
---@field pausedUntil integer
---@field queuedEvents any[][]
---@field waitingForCoroutineYield thread
----@field waitingForProcessIdToExit integer

---@class Process
---@field id integer
---@field description string
---@field currentWorkingDirectory string
---@field args any[]
----@field pausedUntil integer --> handled by scheduler

---@class ProcessWithPrivateFields : Process
---@field unblockedThreads OsThread[]
---@field blockedThreads OsThread[]
----@field state PROCESS_RUNSTATE  --> handled by scheduler maybe
----@field blockingProcesses ProcessWithPrivateFields[] --> handled by scheduler

---@enum PROCESS_RUNSTATE
local PROCESS_RUNSTATE = {
    unstarted = 0,
    runnable = 1,
    running = 2,
}

---@return ProcessWithPrivateFields
function PROCESS.new(desc, path, ...)
    PROCESS.curId = PROCESS.curId + 1
    return setmetatable({
        -- public
        description = desc,
        path = path,
        args = table.pack(...),
        id = PROCESS.curId,
        pausedUntil = -1,
        -- private
        state =  PROCESS_RUNSTATE.unstarted,
        --resumptionArgs = {},
        handlers = {},
        mainThread = nil,
        unblockedThreads = {}, -- all os-threads that are currently resumable
        blockedThreads = {}, -- all os-threads that are currently not resumable
        blockingProcesses = {}, -- processes blocked by this process
    }, {
        __index = PROCESS
    })
end


local syscalls = {}


-- syscalls
function syscalls.spawn(mode, description, path, ...)
    assertPermission(PERMISSION)
    if type(mode) ~= "string" then
        error("process spawning mode must be a string")
    end
    local proc = PROCESS.new(description, path, ...)
    if mode == "blocked" then
        scheduler.block(scheduler.curProcess(), proc)
    elseif mode ~= "background" then
        error("unsupported process spawning mode "..mode)
    end
    scheduler.enqueue(proc)
    return wrap(proc, ALLOWED_READS)
end


return syscalls
