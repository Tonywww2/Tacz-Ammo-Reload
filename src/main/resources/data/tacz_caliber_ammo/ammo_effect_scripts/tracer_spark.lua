-- Tracer spark: spawn a flame particle at the bullet every tick.
-- NOTE: on_bullet_tick runs every tick for EVERY bullet of this ammo.
-- If you do heavy work, throttle it yourself (e.g. only act on some ticks).
local M = {}

function M.on_bullet_tick(api)
    api:particle("minecraft:flame", 1, 0.01)
end

return M
