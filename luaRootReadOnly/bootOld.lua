print("hello world")
for i=1,5 do
    local a = 5
    if a % 3 == 2 then a = a+1 end
    print(tostring(i).."test"..tostring(a))
end