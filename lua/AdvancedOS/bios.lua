_G.components = {}
local idx = 1
for t, a in component.list() do
   print(t, a)
   if t == "disk" then
      --print("has boot file? ", a.fileExists("boot.lua"))
      if a:fileExists("boot.lua") then
         print("Bootable file found on disk #"..idx.." - reading...")
         local code = a:open("boot.lua").read()
         print("Compiling boot.lua...")
         --print(code, type(code))
         _G.bootDrive = a
         local f = load(code)
         if not f then error("bios boot compilation failed") end
         print("Booting...")
---@diagnostic disable-next-line: need-check-nil         
         local ok, err = pcall(f)
         if not ok then print(err)
            error("bios boot error")
         end
         break
      else
         idx = idx+1
      end
   end
end

--error("No bootable filesystem found!")