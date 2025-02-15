local kernel = {}

---@type {["pid"]:integer, ["priority"]:integer, ["coroutine"]:thread}[]
local processes = {}
local nextPid = 1


-- todo add event loop, and then keep resuming coroutines; replace the sleep function to be coroutine.yield; replace coroutine.create such that sleep works in child coroutines too
function kernel:run()
    for pid, proc in pairs(processes) do
        coroutine.resume(proc["coroutine"])
    end
end


function kernel:startProcess(luaPath)
    local f = loadfile(luaPath)
    local pid = nextPid
    nextPid = nextPid+1
---@diagnostic disable-next-line: param-type-mismatch
    table.insert(processes, {pid=pid, priority=0, coroutine=coroutine.create(f)})
    return pid
end

return kernel