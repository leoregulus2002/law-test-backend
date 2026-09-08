local time = redis.call('TIME')
local now = time[1] * 1000 + math.floor(time[2] / 1000)
local expires = tonumber(ARGV[1])
if expires <= now then return 0 end
redis.call('SET', KEYS[1], '1', 'PXAT', expires)
return 1
