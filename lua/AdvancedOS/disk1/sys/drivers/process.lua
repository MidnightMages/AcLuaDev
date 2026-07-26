--[[

]]


local assertPermission = kutils.assertPermission
local wrap = kutils.wrapObjectWithAccess
local unwrap = kutils.unwrapObject
local scheduler = scheduler

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
---@field coroutine thread


---@class ProcessStartInfo
---@field description string
---@field currentWorkingDirectory string
---@field args any[]

---@class FullProcessStartInfo : ProcessStartInfo
---@field mainFunc function
---@field args any[]



---@class Process : ProcessStartInfo
---@field id integer

---@class ProcessWithPrivateFields : Process
---@field unblockedThreads OsThread[]
---@field blockedThreads OsThread[]
---@field createThread function

---@enum PROCESS_RUNSTATE
local PROCESS_RUNSTATE = {
    unstarted = 0,
    runnable = 1,
    running = 2,
}

---@param processStartInfo FullProcessStartInfo
---@return ProcessWithPrivateFields
function PROCESS.new(processStartInfo)
    PROCESS.curId = PROCESS.curId + 1

    assert(getmetatable(processStartInfo) == nil, "processStartInfo cannot have a metatable attached")
    local desc = processStartInfo.description
    local cwd = processStartInfo.currentWorkingDirectory
    local args = {table.unpack(processStartInfo.args)}

    local proc = setmetatable({
        -- public
        description = desc,
        currentWorkingDirectory = cwd,

        id = PROCESS.curId,
        -- private
        unblockedThreads = {}, -- all os-threads that are currently resumable
        blockedThreads = {}, -- all os-threads that are currently not resumable
    }, {
        __index = PROCESS
    })
    proc:createThread(processStartInfo.mainFunc, args)
    return proc
end

function PROCESS:createThread(funcToExecute, packedArgs)
    table.insert(self.unblockedThreads, coroutine.create(function()
        funcToExecute(table.unpack(packedArgs))
    end))
end

local syscalls = {}


-- syscalls


---@param processStartInfo FullProcessStartInfo
function syscalls.spawnProcess(processStartInfo)
    local proc = PROCESS.new(processStartInfo)
    scheduler.enqueue(proc)
    return wrap(proc, ALLOWED_READS)
end

return syscalls
