-- Explosive: a small explosion (radius 2, with fire, no block damage)
-- when the bullet hits an entity or a block.
local M = {}

local function boom(api)
    api:explode(2.0, true, false)
end

M.on_hit_entity = boom
M.on_hit_block = boom

return M
