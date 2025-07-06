local c2 = nil
local a = (b
and 
c<<1 
or 
d 
and 
((function () 
    local cOld = c2; 
    c2 = 4 
    return cOld and cOld:cOld1(cOld.cOld2, {cOld.cOld3,[[aa]], {}, {["bla"]=cOld, 4,2 or 3}}) end)() 
    or 
    function () 
        c2 = 4 
        return c2 
    end))