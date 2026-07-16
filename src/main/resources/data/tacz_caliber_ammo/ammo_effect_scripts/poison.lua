-- Poison: apply Poison I for 5s (100 ticks) to the hit entity.
local M = {}

function M.on_hit_entity(api)
    api:addEffect("minecraft:poison", 100, 0)
end

return M
