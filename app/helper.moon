rprint = (t) ->
  rprint_walker = (t,l) ->
    r = ""
    if l == nil
      l = 0
    if l > 10
      return r
    if type(t) == "table"
      for k,v in pairs(t)
        
        if k == nil
          k = ""
        r ..= "\n"
   
        for i=1,l do
           r ..= "  "
        if v == nil
          v = ""

        if type(k) == "table"
          k = "{}"
        
        tout = rprint_walker(v,l+1)
        if not tout
          tout = "nil"
        r ..= k .. "(" .. type(k) .. ") = " .. tout
        else
      r ..= (tostring t) .. " (" .. type(t) .. ")"
    r

  out = rprint_walker t
  if not out
    out = "nil"
  print "\n" .. out .. "\n\n"

{
  :rprint 
}
