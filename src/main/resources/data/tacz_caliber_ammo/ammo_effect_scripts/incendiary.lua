-- Incendiary: ignite the hit entity for 6s and mark it glowing.
-- Bind by setting an ammo's effects.script to "tacz_caliber_ammo:incendiary".
local M = {}

function M.on_hit_entity(api)
    api:ignite(6)
    api:addEffect("minecraft:glowing", 100, 0)
end

return M
