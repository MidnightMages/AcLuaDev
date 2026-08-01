--[[
A little implementation of a text editor mimicing the gnu nano editor
]]

local DEFAULT_NAME <const> = "unnamed.txt"
local filename = ... -- the first argument is either a "-h" or the file name

if filename == "-h" or filename == "-?" then
    -- print help
    print([[Advanced OS - NANO:
    nano [ <filename> | -h | -? ]
      -h / -?     print this help page
      <filename>  the file to open (it does not need to exist)]])
    return
end

local fs = require "filesystem"
local kernel = require "kernel"
local insert = table.insert

local debugPrints = {}
local function print(...)
    debugPrints[#debugPrints + 1] = table.pack(...)
end


-- try loading the file
local workingDir = kernel:getCurrentProcess().currentWorkingDirectory
local function tryResolveFileName(filename)
    if string.startsWith(filename, "/") then
        return filename
    elseif string.startsWith(filename, "./") then
        return workingDir .. string.sub(filename, 3)
    else
        return workingDir .. filename
    end
end
local data = {}
if filename ~= nil then
    filename = tryResolveFileName(filename)
    if fs:fileExists(filename) then
        data = string.split(fs:readAllText(filename), "\n")
        -- normalize line endings from possibly \r\n to \n
        for i = 1, #data do
            local l = data[i]
            if string.endsWith(l, "\r") then
                data[i] = string.sub(l, 1, #l - 1)
            end
        end
    end
end
if #data == 0 then
    insert(data, "")
end
local buffer = kernel:getCurTextBuffer()


-- helpers
local function padRight(str, len, filler)
    filler = filler or " "
    local res = str .. string.rep(filler, math.max(0, (len - #str) // #filler))
    return #res >= len and res or res .. string.sub(filler, 1, len - #res)
end
local function padLeft(str, len, filler)
    filler = filler or " "
    local res = string.rep(filler, math.max(0, (len - #str) // #filler)) .. str
    return #res >= len and res or string.sub(filler, 1, len - #res) .. res
end

-- set up screen
local WIDTH <const> = buffer.width
local HEIGHT <const> = buffer.height
local VISIBLE_LINES <const> = HEIGHT - 3
local previousScreen = buffer:getTextAsString()
buffer:pasteText(0, 0, "FILL_CLIP_CLEAR", string.rep("\n", HEIGHT))
-- state
local running = true
local scrollPos = 1
local showLineNums = true
-- caret
local cy = 1
local cx = 1
local hiddenByCaret = nil
local function toScreenPos(x, y)
    return math.min(x + (showLineNums and #tostring(#data) + 1 or 0), WIDTH) - 1, scrollPos - 1 + y
end
local function clearCaret()
    if hiddenByCaret then
        -- restore character
        local x, y = toScreenPos(cx, cy)
        buffer:set(x, y, hiddenByCaret, nil, nil)
        hiddenByCaret = nil
    end
end
-- top line
local shortFileName = filename == nil and DEFAULT_NAME
    or #filename <= 32 and filename
    or string.sub(filename, 1, 32)
shortFileName = filename or DEFAULT_NAME
buffer:pasteText(0, 0, "STOP", padRight(
    string.format("███ AdvancedOS NANO ███ %s ███", shortFileName), WIDTH, "█"))
-- bottom line
buffer:pasteText(0, HEIGHT - 1, "^O Write Out    ^X Exit         ^L Line Numbers ")
local function updateLineCnt()
    buffer:pasteText(0, HEIGHT - 2, "STOP_CLEAR", padRight(
        string.format("███ [ the file has %d lines ] ", #data),
        WIDTH, "█"))
end
updateLineCnt()
-- line helpers
local function drawLines()
    -- move screen to caret
    -- TODO handle offscreen in x
    local cdif = cy - scrollPos
    if cdif > VISIBLE_LINES then
        scrollPos = scrollPos + (cdif - VISIBLE_LINES)
    elseif cdif < 0 then
        scrollPos = scrollPos + (-cdif - VISIBLE_LINES)
    end
    -- print the lines
    local lines = table.move(data, scrollPos, VISIBLE_LINES, 1, {})
    if showLineNums then
        local maxlen = #tostring(#data)
        for i = 1, #lines do
            local l = padLeft(tostring(i), maxlen) .. " " .. lines[i]
            l = #l <= WIDTH and l or string.sub(i, #l - 1) .. ">"
            lines[i] = l
        end
    else
        for i = 1, #lines do
            local l = lines[i]
            l = #l <= WIDTH and l or string.sub(i, #l - 1) .. ">"
        end
    end
    for i = #lines + 1, VISIBLE_LINES do
        lines[#lines + 1] = ""
    end
    buffer:pasteText(0, 1, "FILL_CLIP_CLEAR", table.concat(lines, "\n"))
end
drawLines()





-- handlers
local function charTyped(char)
    clearCaret()
    local l = data[cy]
    if char == "\n" then
        -- new line
        if cx <= 1 then
            -- move entire line down
            table.insert(data, cy, "")
        elseif cx > #l then
            -- clean newline
            table.insert(data, cy + 1, "")
        else
            -- split the old line
            local old = string.sub(l, 1, cx - 1)
            local new = string.sub(l, cx, #l)
            data[cy] = old
            table.insert(data, cy + 1, new)
        end
        cx = 1
        cy = cy + 1
        updateLineCnt()
    elseif char == "\b" then
        -- remove char
        if cx <= 1 then
            -- start of the line, wrap
            if cy > 1 then
                -- there is a line above
                table.remove(data, cy)
                cy = cy - 1
                l = data[cy] .. l
                cx = #l + 2
            end
        elseif cx == 2 then
            -- first char
            l = string.sub(l, 2, #l)
        elseif cx > #l then
            -- end of the line
            l = string.sub(l, 1, #l - 1)
        else
            -- middle of the line
            l = string.sub(l, 1, cx - 2) .. string.sub(l, cx, #l)
        end
        data[cy] = l
        cx = cx - 1
    else
        if cx <= 1 then
            -- start of the line
            l = char .. l
        elseif cx > #l then
            -- end of the line
            l = l .. char
        else
            -- middle of the line
            l = string.sub(l, 1, cx - 1) .. char .. string.sub(l, cx, #l)
        end
        data[cy] = l
        cx = cx + 1
    end
    drawLines()
end

local function keyPressed(stRep, keyCode, scanCode, mods)
    clearCaret()
    if keyCode == 266 then     -- PAGE_UP
        cy = math.max(1, cy - VISIBLE_LINES)
    elseif keyCode == 267 then -- PAGE_DOWN
        cy = math.min(#data, cy + VISIBLE_LINES)
    elseif keyCode == 268 then -- HOME
        cx = 1
    elseif keyCode == 269 then -- END
        cx = #data[cy] + 1
    elseif keyCode == 262 then -- RIGHT
        if cx > #data[cy] then
            -- wrap line
            if cy < #data then
                cy = cy + 1
                cx = 1
            end
        else
            cx = cx + 1
        end
    elseif keyCode == 263 then -- LEFT
        if cx <= 1 then
            -- wrap line
            if cy > 1 then
                cy = cy - 1
                cx = #data[cy]
            end
        else
            cx = cx - 1
        end
    elseif keyCode == 264 then                -- DOWN
        cy = math.min(#data, cy + 1)
    elseif keyCode == 265 then                -- UP
        cy = math.max(1, cy - 1)
    elseif keyCode == 0x4C and mods == 2 then -- ^L
        -- toggle line numbers
        showLineNums = not showLineNums
        drawLines()
    elseif keyCode == 0x4F and mods == 2 then -- ^O
        -- save
        -- TODO handle missing file name
    elseif keyCode == 0x58 and mods == 2 then -- ^X
        -- quit
        -- TODO handle unsaved changes
        running = false
    end
    local cdif = cy - scrollPos
    if cdif > VISIBLE_LINES or cdif < 0 then
        -- caret is off screen in y
        -- TODO handle offscreen in x
        drawLines()
    end
end

local function textPasted(text)
    -- TODO
end



-- register handlers
kernel:registerEventCallback("keyTyped", function(...)
    charTyped(select(2, ...))
end)

kernel:registerEventCallback("keyPressed", function(...)
    keyPressed(select(2, ...))
end)

--[[
kernel:registerEventCallback("textPasted", function(...)
    ---if keyTyped(select(2,...)) then keepRunning = false end
end)
]]

while running do
    sleep(0.5)
    local x, y = toScreenPos(cx, cy)
    -- blink the caret
    if hiddenByCaret then
        -- restore character
        buffer:set(x, y, hiddenByCaret, nil, nil)
        hiddenByCaret = nil
    else
        hiddenByCaret = buffer:getText(x, y)
        buffer:set(x, y, "_", nil, nil)
    end
end

--exit
buffer:pasteText(0, 0, "FILL_CLIP_CLEAR", previousScreen)

for _, dbg in ipairs(debugPrints) do
    _ENV.print("[nano DEBUG]", table.unpack(dbg))
end
