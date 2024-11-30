local idx = 1
for t, a in component.list() do
   print(t, a)
   if t == "disk" then
      --print("has boot file? ", a.fileExists("boot.lua"))
      if a.fileExists("boot.lua") then
         print("Bootable file found on disk #"..idx.." - reading...")
         local code = a.open("boot.lua").read()
         print("compiling...")
         --print(code, type(code))
         local f = load(code)
         print("executing...")
---@diagnostic disable-next-line: need-check-nil
         f()
         break
      else
         idx = idx+1
      end
   end
end

--error("No bootable filesystem found!")