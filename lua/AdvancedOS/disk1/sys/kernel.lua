--[[
This is the base execution loop for the scheduler
]]

local queue = {}
scheduler = {}

function scheduler.enqueue(proc)
    
end

function scheduler.block(blocked, blocking)
    
end




while true do
    local proc = table.remove(queue, 1)
    local result = table.pack(proc.curThread.resume())
    if not result[1] then
        -- this coroutine resulted in an error
        if #proc.blockedThreads > 0 then
            -- we have stuff to resume
            proc.curThread = table.remove(proc.blockedThreads, #proc.blockedThreads)
            proc.resumptionArgs = result
        else
            -- no remaining coroutine, we need to kill the process
            -- TODO
        end
    else
        -- success
        if result[2] == "yield" then
            -- we yield to the last coroutine
            proc.curThread = table.remove(proc.blockedThreads, #proc.blockedThreads)
            table.remove(result, 2) -- cutting out the "yield" field added as a syscall
            proc.resumptionArgs = result
            scheduler.enqueue(proc)
        elseif result[2] == "resume" then
            -- the current coroutine becomes blocked by the new one
            table.insert(proc.blockedThreads, proc.curThread)
            proc.curThread = result[3]
            table.remove(result, 3) -- remove the coroutine
            table.remove(result, 2) -- remove "resume"
            table.remove(result, 1) -- remove resumption success
            proc.resumptionArgs = result
            scheduler.enqueue(proc)
        elseif result[2] == "shutdown" then
            -- TODO check permissions
            break
        elseif result[2] == "syscall" then
            -- TODO handle syscall
        end
    end
end

print("shutting down ...")