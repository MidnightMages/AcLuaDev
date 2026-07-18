local c = components
local var1 = c:getFirst("gpu")
local buf = var1:newBuffer(110, 44)
var1:assignBuffer(buf,c:getFirst("screen"))
var1 = c:getFirst("computer")

local cursorX, cursorY = 0, 0
local t = table
function write(...)
    cursorX, cursorY = buf:pasteText(cursorX, cursorY, "SCROLL_SPILL_CLEAR", t.concat(t.pack(...), " "))
end
function print(...)
    write(t.concat(t.pack(...)," ").."\n")
end
local w = write
local p = print
p("Lua shell:")

w(">>> ")
local line = ""
local caretShown = false
local untilCaretFlip = 0
while""do
    local e1,stringRepr=var1:getMachineEvent()
    if not e1 then
        sleep(0.05)
        untilCaretFlip = untilCaretFlip - 0.05
        if untilCaretFlip < 0 then
            w(caretShown and "\b" or "_")
            caretShown = not caretShown
            untilCaretFlip = 0.5
        end
    elseif e1 == "shutdown" then
        break
    elseif e1 == "keyPressed" then
        if stringRepr ~= "" then
            if caretShown then
                caretShown = false
                w("\b")
                untilCaretFlip = 0
            end
            if stringRepr == "\b" then
                if #line > 0 then
                    w("\b")
                    line = line:sub(1,-2)
                end
            elseif stringRepr == "\n" then
                w("\n")
                local f, err = load(line)
                if not f then
                    p("Error: "..err)
                else
                    local ok, res = xpcall(f, debug.traceback)
                    if not ok then
                        p("Error: ",tostring(res))
                    end
                end
                w(">>> ")
                line=""
            else
                w(stringRepr)
                line = line .. stringRepr
            end
        end
    end
end
