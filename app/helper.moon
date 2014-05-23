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

      r ..= k .. "(" .. type(k) .. ") = " .. rprint_walker(v,l+1)
  else
    r ..= (tostring t) .. " (" .. type(t) .. ")"
  r

rprint = (t) ->
  print "\n" .. (rprint_walker t)  .. "\n\n"

{ :rprint }
