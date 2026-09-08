local time = redis.call('TIME')
local now = time[1] * 1000 + math.floor(time[2] / 1000)
if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
if redis.call('HGET', KEYS[1], 'family') ~= ARGV[3]
    or redis.call('HGET', KEYS[1], 'user') ~= ARGV[2]
    or redis.call('HGET', KEYS[1], 'id') ~= ARGV[4] then return 0 end
if redis.call('HGET', KEYS[1], 'used') ~= '' then
    for _, member in ipairs(redis.call('ZRANGE', KEYS[3], 0, -1)) do
        redis.call('DEL', member)
    end
    redis.call('DEL', KEYS[1], KEYS[3])
    return -1
end
local expires = tonumber(ARGV[6])
if tonumber(redis.call('HGET', KEYS[1], 'expires')) <= now or expires <= now then return 0 end
if redis.call('EXISTS', KEYS[2]) == 1 or redis.call('EXISTS', KEYS[3]) == 0 then return 0 end
-- 保留旧令牌及其原 TTL，以便在原有效期内识别重放。
redis.call('HSET', KEYS[1], 'used', now)
redis.call('HSET', KEYS[2], 'id', ARGV[1], 'user', ARGV[2], 'family', ARGV[3],
    'previous', ARGV[4], 'created', ARGV[5], 'expires', ARGV[6], 'used', '')
redis.call('PEXPIREAT', KEYS[2], expires)
redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', now)
redis.call('ZADD', KEYS[3], expires, KEYS[2])
local last = redis.call('ZRANGE', KEYS[3], -1, -1, 'WITHSCORES')
redis.call('PEXPIREAT', KEYS[3], tonumber(last[2]))
return 1
