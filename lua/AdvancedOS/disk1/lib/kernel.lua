local kernel = {}

---@type process?
local currProcess = nil
---@type scheduledThread?
local currScheduledThread = nil
function kernel:registerEventCallback(eventName, callback) -- TODO add unregister function
    self:invokeSyscall("registerProcessEventCallback", eventName, callback)
end

function kernel:invokeSyscall(syscallName, ...)
    return coroutine.yield("syscall", syscallName, ...)
end

function kernel:debug(...)
    if true then
        print("[D]",...)
    end
end

---@return Process
function kernel:getCurrentProcess()
    return self:invokeSyscall("getCurrentProcess")
end

local nextPid = 1
---@param proc processStartData
function kernel:startProcess(proc)
    local pid = nextPid
    nextPid = nextPid+1
    ---@type process
    local processData = proc
    processData.pid = pid
    local handle = {pid=pid, result = nil, state="running"}
    processData.handle = handle
    table.insert(processes, processData)
    run_skipCurrentSleep = true
    return handle
end

-- ---@param luaPath string
-- ---@param argString string?
-- function kernel:startProcessFromPath(luaPath, argString)
--     debugf("starting", luaPath)
--     local f = assert(loadfile(luaPath), "failed to load file")
--     local psplits = string.split(luaPath,"/")
--     return kernel:startProcess({
--         priority=0, 
--         coroutines={{coroutine=coroutineXPCreate(f), resumeAfter=-1}}, 
--         cwd=(currProcess and currProcess.cwd) or (table.concat(psplits, "/", 1, #psplits-1).."/"), 
--         resumeAfter=-1, 
--         args=argString or "",
--         name = luaPath
--     })
-- end

---@param luaPath string
---@param args {}
function kernel:startProcessFromPath(luaPath, args)
    ---@type FullProcessStartInfo
    local startInfo = {
        mainFunc = assert(loadfile(luaPath)),
        args = args,
        currentWorkingDirectory = self:getCurrentProcess().currentWorkingDirectory,
        description = tostring(luaPath)
    }
    return self:invokeSyscall("spawnProcess", startInfo)
end

---@param processHandle Process
function kernel:waitForProcessExit(processHandle)
    --print("waiting for ",processHandle.description ," to finish")
    while processHandle.state ~= PROCESS_RUNSTATE.dead do
        --print("sleep begun", processHandle.state)
        sleep(0.1)
    end
    return processHandle.endedSuccessfully
end

---@returns string
function kernel:getCurrentWorkingDirectory()
    return kernel:getCurrentProcess().currentWorkingDirectory
end

---@param s string
---@return string
function kernel:normalizePath(s)
    local splitted = string.split(s, "/")
    local rv = ""
    local skipCnt = 0
    for i = #splitted, 1, -1 do
        local seg = splitted[i]
        if i > 1 and #seg == 0 then
            goto continue
        end
        if seg == ".." then
            skipCnt = skipCnt + 1
        elseif seg ~= "." then
            if skipCnt > 0 then
                skipCnt = skipCnt -1
            else
                if i == #splitted then
                    rv = seg
                else
                    rv = seg .. "/" .. rv
                end
            end
        end
        --print("seg", seg, rv)
        ::continue::
    end
    
    if skipCnt > 0 then
        return "/"
    end
    return rv
end

---@param newCwd string
function kernel:setCurrentWorkingDirectory(newCwd)
    newCwd = newCwd or "/"
    if newCwd:sub(1, 1) ~= "/" then
        newCwd = "/" .. newCwd
    end
    if newCwd:sub(#newCwd, #newCwd) ~= "/" then
        newCwd = newCwd .. "/"
    end

    kernel:getCurrentProcess().cwd = self:normalizePath(newCwd)
end

function kernel:getCurTextBuffer()
    -- TODO do not give up control over the UEFI text buffer and user one buffer per process instead
    -- maybe allow the process to maintain and show different text buffers
    return _ENV.uefiTextBuffer -- this is set in uefi.lua
end

return kernel