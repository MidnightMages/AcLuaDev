_G.func=function(input,space)
    if not space then
        space=""
    end
    for i,v in pairs(input) do
        print(space,i,v)
        if type(v) == "table" and i ~= "_G" then
            func(v,space.."    ")
        end
    end
end