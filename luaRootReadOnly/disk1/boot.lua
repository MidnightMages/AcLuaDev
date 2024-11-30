--local a = {}
--setmetatable(a,a)
--a[1] = 1
local function pp(tbl)
    for k,v in pairs(tbl) do
        print(k, ":", v)
    end
end
pp(_G)

xpcall = function (...)
    return ...
end

local function truncLast(s)
    local rv = ""
    for i=1,#s-1 do
        rv = rv .. string.sub(s,i,i)
    end
    return rv
end

local stringBuffer = ""
local function keyTyped(key) -- return whether to exit
    if key == "\b" then
        if #stringBuffer > 0 then
            printInline(key)
            stringBuffer = truncLast(stringBuffer) --stringBuffer:sub(1, #stringBuffer - 1)
        end
    else
        printInline(key)
    end

    if key == "\n" then
        if stringBuffer == "exit()" then
            return true
        end
        local res, err = load(stringBuffer, _G)
        stringBuffer = ""
        if not res then
            print("Error: ", err)
        else
            res() --[[
            local rvs = table.pack(xpcall(res,function(msg)
                local trcb = debug.traceback("X-ERR: " .. tostring(msg), 2)
                for i = 1, 4, 1 do
                    trcb = trcb:sub(1, trcb:match("^.*()\n") - 1)
                end
                print(trcb)
            end))]]
        end
        printInline(">> ")
    elseif key ~= "\b" then
        stringBuffer = stringBuffer .. key
    end
end

while true do
    local event, a1, a2 = computer.getMachineEvent()
    if event == nil then
        sleep(0.05)
    elseif event == "shutdown" then
        break
    elseif event == "keyTyped" then
        if keyTyped(a1) then break end
    end
end

--local input = readline("Please enter a cool input:")

print("You typed", input)
-- init filesystem
-- init shell
-- run autorun.lua files