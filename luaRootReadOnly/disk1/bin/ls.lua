local args = ...
local kernel = require("kernel")
local fs = require("filesystem")

local dir = kernel:getCurrentProcess().cwd
print("./")
print("../")
for _,f in ipairs(fs:listChildren(dir)) do
    print(f)
end
