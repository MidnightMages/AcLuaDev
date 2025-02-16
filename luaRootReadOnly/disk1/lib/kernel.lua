local kernel = {}

--@alias process {["pid"]:integer, ["priority"]:integer, ["coroutine"]:thread, ["cwd"]:string}
---@alias process {pid:integer, priority:integer, coroutine:thread, cwd:string}


----@type {["pid"]:integer, ["priority"]:integer, ["coroutine"]:thread, ["cwd"]:string}[]

---@type process[]
local processes = {}
local nextPid = 1

-- todo add event loop, and then keep resuming coroutines; replace the sleep function to be coroutine.yield; replace coroutine.create such that sleep works in child coroutines too

local currProcess = nil
function kernel:run()
    for pid, proc in pairs(processes) do
        currProcess = proc -- TODO lock this table or make a clone, so that it cannot be edited
        coroutine.resume(proc.coroutine)
        currProcess = nil
    end
end

---@return process
function kernel:getCurrentProcess() return assert(currProcess) end

function kernel:startProcess(luaPath)
    local f = loadfile(luaPath)
    local pid = nextPid
    nextPid = nextPid+1
    local psplits = string.split(luaPath,"/")
---@diagnostic disable-next-line: param-type-mismatch
    table.insert(processes, {pid=pid, priority=0, coroutine=coroutine.create(f), cwd=table.concat(psplits, "/", 1, #psplits-1).."/"})
    return pid
end

return kernel