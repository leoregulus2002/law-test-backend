if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
local count = redis.call('INCR', KEYS[1])
if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end
if count >= tonumber(ARGV[1]) then
    redis.call('SET', KEYS[2], '1', 'PX', ARGV[3])
    redis.call('DEL', KEYS[1])
end
return count
