local computer = components:getFirst("computer")

---@type ProcessWithPrivateFields | nil
local currentlyRunningProcess = nil -- nil = kernelContext
local registeredPermissions = {}
function kutils.registerPermission(name)
    registeredPermissions[name] = true
end

function kutils.assertPermission(name)
    assert(registeredPermissions[name] ~= nil, "permission "..tostring(name).." has not been registered!")
    return (currentlyRunningProcess == nil) --or (currentlyRunningProcess.euid == 0)
end

local syscalls = {}

local function registerDriver(path)
    local newSyscalls = dofile(path)
    assert(type(newSyscalls) == "table", "driver did not return a syscall table")
    for key, value in pairs(newSyscalls) do
        assert(syscalls[key] == nil, "syscall "..tostring(key).." is already registered!")
        syscalls[key] = value
    end
end

--[[
This is the base execution loop for the scheduler
]]


---@type ProcessWithPrivateFields[]
local runningProcesses = {}
_ENV.scheduler = {
    ---@type table<string, table<ProcessWithPrivateFields, function[]>>
    registeredEventCallbacksByTypeAndProcess = {}
}

function scheduler:enqueue(proc)
    table.insert(runningProcesses, proc)
end

function scheduler:block(blocked, blocking)
    
end

function scheduler:registerEventCallback(eventName, callbackFunc)
    
end

---@param process ProcessWithPrivateFields
---@param func function
---@param ... any
function scheduler:spawnNewThreadInProcess(process, func, ...) -- ... = thread start args
    process:createThread(func, table.pack(...))
end

registerDriver("/sys/drivers/process.lua")

function panic(msg)
    -- TODO
end
function doSyscall()
    -- TODO
end




print("new kernel running!!!!!!")

local function runTasks()
    while true do
        -- process events
        while true do
            local machineEvent = {computer:getMachineEvent()}
            if #machineEvent == 0 then break end -- no event available

            if machineEvent[1] == "shutdown" then
                return
            end

            for _, process in ipairs(runningProcesses) do -- walk through all registered handlers and spawn new threads
                for i = 1, 2 do
                    local handlers = (scheduler.registeredEventCallbacksByTypeAndProcess[i == 1 and "*" or machineEvent[1]] or {})[process] or {}
                    for j = 1, #handlers do
                        scheduler:spawnNewThreadInProcess(process,handlers[j],table.unpack(machineEvent))
                    end
                end
            end
            -- resume all eventhandlers
        end

        for i = 1, #runningProcesses do
            local processToRun = runningProcesses[i]
            currentlyRunningProcess = processToRun
            local unblockedThreads = processToRun.unblockedThreads
            for  j = 1, #unblockedThreads do
                local currThreadToRun = unblockedThreads[j]
                if (currThreadToRun.pausedUntil or -1) < computer.getEpoch() then
                    local result = table.pack(coroutine.resume(currThreadToRun.coroutine))

                    -- handle syscalls / result
                    if not result[1] then -- if error
                        -- TODO kill process
                            error("we need to kill a process (proc errored) :(")
                    else -- success
                        local action = result[2]
                        if action == "syscall" then
                            local syscallName = result[3]
                            if syscallName == "sleep" then
                                local sleepDuration = result[4]
                                assert(type(sleepDuration) == "number")
                                currThreadToRun.pausedUntil = computer.getEpoch() + tonumber(sleepDuration)
                            end
                        else
                            error("we need to kill a process (bad syscall) :(")
                            -- TODO kill process
                        end
                    end
                end
            end
            currentlyRunningProcess = nil
            sleep(0.05)
        end
    end
end

---@type FullProcessStartInfo
local initProcessStartInfo = {
    currentWorkingDirectory = "/bin/",
    mainFunc = assert(loadfile("/bin/sh.lua")),
    args = {},
    description = "init shell"
}
syscalls.spawnProcess(initProcessStartInfo)
runTasks()
print("shutting down ...")