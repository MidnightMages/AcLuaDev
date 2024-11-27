--local a = {}
--setmetatable(a,a)
--a[1] = 1
local function pp(tbl)
    for k,v in pairs(tbl) do
        print(k, ":", v)
    end
end
pp(_G)

local input = readline("Please enter a cool input:")

print("You typed", input)
-- init filesystem
-- init shell
-- run autorun.lua files