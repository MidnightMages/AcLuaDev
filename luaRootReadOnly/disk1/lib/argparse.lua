local ap = {}

function ap:extractQuotedSegments(...)
    local str = string.join(" ", ...)
    local qchar = nil
    local rv = {}
    local buf = ""
    local lastC = nil
    local c = nil
    for i = 1, #str do
        local lastC = c
        c = str:sub(i,i)

        if c == "\\" then
            if isEscaped then
                
            end
        end

        if qchar == nil and c == " " then
            table.insert(rv, buf)
            buf = ""
        end
        if (c == "'" or c == '"') and not isEscaped then
            
        end
    end
end

return ap