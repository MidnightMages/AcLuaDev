local kernel = {}

---@class processStartData : table
---@field priority integer
---@field coroutine thread
---@field cwd string
---@field resumeAfter number

---@class process : processStartData
---@field pid integer

---@type process[]
local processes = {}

-- todo add event loop, and then keep resuming coroutines; replace the sleep function to be coroutine.yield; replace coroutine.create such that sleep works in child coroutines too

local shutdownRequested = false
local eventQueue = {}
--[[local function eventPump()
    local event, a1, a2 = computer.getMachineEvent()
    if event ~= nil then        
        if event == "shutdown" then

        elseif event == "keyTyped" then
            if keyTyped(a1) then break end
        end
    end
end]]

--eventId : delegate[]
---@type table<string, function[]>
local eventHandlers = {}

local time = 0
local function os_time() -- TODO replace with computer.time() once it is implemented
    return time
end

function kernel:registerEventCalback(eventName, callback) -- TODO add unregister function
    local container = eventHandlers[eventName]
    if not container then
        container = {}
        eventHandlers[eventName] = container
    end
    table.insert(container, callback)    
end

---@type process?
local currProcess = nil
local sleepRaw = sleep
function kernel:run()
    --kernel:startProcess({priority=0, coroutine=coroutine.create(eventPump), cwd="/"})
    
    local processIdleTimeLeft = 0
    while not shutdownRequested do

        while true do -- process event queue always
            local nextEvent = table.pack(computer.getMachineEvent())
            if nextEvent[1] == nil then break end
            if nextEvent[1] == "shutdown" then
                shutdownRequested = true
            end
            local container = eventHandlers[nextEvent[1]]
            if container then
                for _, f in ipairs(container) do
                    f(table.unpack(nextEvent))
                end
            end
        end

        if processIdleTimeLeft <= 0 then
            local earliestResume = nil
            for pid, proc in pairs(processes) do
                if proc.resumeAfter < os_time() then
                    currProcess = proc -- TODO lock this table or make a clone, so that it cannot be edited
                    --print("resuming", coroutine.status(proc.coroutine))
                    coroutine.resume(proc.coroutine)
                    local resumeAt =  currProcess.resumeAfter
                    earliestResume = earliestResume and math.min(earliestResume, resumeAt) or resumeAt
                    currProcess = nil
                end
            end
            processIdleTimeLeft = math.min(1, earliestResume and (earliestResume-os_time()) or 0) -- pause process queue execution at most for one second
        end
        
        local sleepAmount = 0.05
        sleepRaw(sleepAmount)
        time = time + sleepAmount
        processIdleTimeLeft = processIdleTimeLeft - sleepAmount
    end
end

---@param duration number Duration in seconds
sleep = function(duration)
    currProcess.resumeAfter = os_time() + math.max(0, duration)
    coroutine.yield()
end

---@return process
function kernel:getCurrentProcess() return assert(currProcess) end

local nextPid = 1
---@param proc processStartData
function kernel:startProcess(proc)
    local pid = nextPid
    nextPid = nextPid+1
    proc["pid"] = pid
    table.insert(processes, proc)
    return pid
end

---@param luaPath string
function kernel:startProcessFromPath(luaPath)
    local f = assert(loadfile(luaPath), "failed to load file")
    local psplits = string.split(luaPath,"/")
    return kernel:startProcess({priority=0, coroutine=coroutine.create(f), cwd=table.concat(psplits, "/", 1, #psplits-1).."/", resumeAfter=-1})
end

return kernel