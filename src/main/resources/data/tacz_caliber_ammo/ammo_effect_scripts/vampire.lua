-- Vampire: on kill, heal the shooter by 4 and play a level-up sound.
local M = {}

function M.on_kill(api)
    api:healShooter(4.0)
    api:sound("minecraft:entity.player.levelup", 0.6, 1.4)
end

return M
