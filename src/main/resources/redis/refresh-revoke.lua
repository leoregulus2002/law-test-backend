local family = redis.call('HGET', KEYS[1], 'family')
if not family then return 0 end
local familyKey = 'auth:refresh-family:' .. family
for _, member in ipairs(redis.call('ZRANGE', familyKey, 0, -1)) do
    redis.call('DEL', member)
end
redis.call('DEL', KEYS[1], familyKey)
return 1
