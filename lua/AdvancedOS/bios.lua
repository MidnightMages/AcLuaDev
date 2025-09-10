local ok, rv = xpcall(function()
	_G.components = {}
	local idx = 1
	for t, a in component.list() do
	   print(t, a.id)
	   if t == "disk" then
		  --print("has boot file? ", a.fileExists("boot.lua"))
		  if a:fileExists("boot.lua") then
			 print("Bootable file found on disk #"..idx.." - reading...")
			 local code = a:open("boot.lua").read()
			 print("Compiling boot.lua...")
			 --print(code, type(code))
			 _G.bootDrive = a
			 local f = load(code, "boot.lua")
			 if not f then error("bios boot compilation failed") end
			 print("Booting...")
	---@diagnostic disable-next-line: need-check-nil         
			 local ok, err = xpcall(f, debug.traceback)
			 if not ok then
				local etext = "bios boot error: "..tostring(err)
				print(etext)
				error(etext)
			 end
			 break
		  else
			 idx = idx+1
		  end
	   end
	end 
end, debug.traceback)
if not ok then
	error("bios error: "..tostring(rv), 0)
end

--error("No bootable filesystem found!")