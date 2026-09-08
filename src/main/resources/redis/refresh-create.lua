local time = redis.call('TIME')
local now = time[1] * 1000 + math.floor(time[2] / 1000)
local expires = tonumber(ARGV[6])
if expires <= now or redis.call('EXISTS', KEYS[1], KEYS[2]) > 0 then return 0 end
redis.call('HSET', KEYS[1], 'id', ARGV[1], 'user', ARGV[2], 'family', ARGV[3],
    'previous', ARGV[4], 'created', ARGV[5], 'expires', ARGV[6], 'used', '')
redis.call('PEXPIREAT', KEYS[1], expires)
redis.call('ZADD', KEYS[2], expires, KEYS[1])
redis.call('PEXPIREAT', KEYS[2], expires)
return 1
