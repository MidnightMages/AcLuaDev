for t, a in component.list() do
   print(t, a)
   if t == "disk" then
      print("has boot file? ", a.fileExists("boot.lua"))
   end
end