--[[

]]


local assertPermission = kutils.assertPermission
local wrap = kutils.wrapObjectWithAccess
local unwrap = kutils.unwrapObject

local DRIVER_NAME <const> = "process"
local PERMISSION <const> = "drv.process"
kutils.registerPermission(PERMISSION)


local PROCESS <const> = {
    curId = 0
}

local ALLOWED_READS = {
    description = true,
    path = true,
    args = true,
    id = true,
    pausedUntil = true,
}

function PROCESS.new(desc, path, ...)
    local id = PROCESS.curId
    PROCESS.curId = id + 1
    return setmetatable({
        -- public
        description = desc,
        path = path,
        args = table.pack(...),
        id = id,
        pausedUntil = -1,
        -- private
        state = "runnable",
        resumptionArgs = {},
        handlers = {},
        mainThread = nil,
        curThread = nil,
        blockedThreads = {},
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
