local data = {
    bootSeq={
        ["1"]="Hyperion",
        ["2"]="AdvancedOS"
    },
    bootCfg={
        Hyperion={bootDrive="disk_1",bootPath="boot/Hyprkrnl.sys",globals={test="globals work"}},
        AdvancedOS={bootDrive="disk_2",bootPath="boot.lua",globals={}}
    }
}
local firstBoot = false
if data == nil then firstBoot = true end
local function Table2Lua(table)
    local output = "{"
    for i,v in pairs(table) do
        local coma=true
        if type(i) == "string" then
            output=output.."[\""..i.."\"]="
        end
        if type(v) == "table" then
            if v == table then
                output=string.sub(output,1,#output-(#i+1))
                coma=false
            else
                output=output..Table2Lua(v)
            end
        elseif type(v) == "string" then
            output=output.."\""..v.."\""
        elseif type(v) == "number" then
            output=output..tostring(v)
        elseif type(v) == "function" then
            output=string.sub(output,1,#output-(#i+1))
            coma=false
        end
        if coma then
            output=output..","
        end
    end
    if #table>0 or string.sub(output,#output,#output) == "," then
        output=string.sub(output,1,#output-1)
    end
    output=output.."}"
    return output
end
local function save()
    component.getFirst("bios"):setSaveData(Table2Lua(data))
end
local function copy(tabl)
    local out = {}
    for i,v in pairs(tabl) do
        local t=type(v)
        if t=="table" then
            if i == "_G" then
                out._G=out
            else
                out[i]=copy(v)
            end
        else
            out[i]=v
        end
    end
    return out
end
local err = ""
local exit = false
if not firstBoot then
    for i,v in pairs(data.bootSeq) do
        print('Trying option '..i," Labeled "..v)
        for t, a in component.list() do
            if t == "disk" then
                if a.id == data.bootCfg[v].bootDrive then
                    local code = a:open(data.bootCfg[v].bootPath).read()
                    local VG = copy(_G)
                    for name,value in pairs(data.bootCfg[v].globals) do
                        VG[name]=value
                        print("added "..name.." to _G")
                    end
                    print("Compiling kernel...")
                    local func = load(code,data.bootCfg[v].bootDrive.."|"..data.bootCfg[v].bootPath,nil,VG)
                    if not func then err = "Compilation error"; print("Compilation failed trying next boot option") end
                    local bootArgs=data.bootCfg[v].bootArgs or {}
                    if func then
                        local ok = true
                        bootArgs.bootDrive=a
                        ok,err = pcall(func,bootArgs)
                        if not ok then
                            if err ~= "exit" then
                                print("Kernel exited with error:")
                                print("\""..err.."\"")
                                print("Trying next boot option")
                            else
                                print("kernel Exited")
                                exit = true
                            end
                        end
                    end
                    break
                end
            end
        end
        if exit then break end
    end
else
    print("")
end