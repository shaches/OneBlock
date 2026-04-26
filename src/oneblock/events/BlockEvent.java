package oneblock.events;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.util.Vector;

import oneblock.Oneblock;
import oneblock.PlayerInfo;

public class BlockEvent implements Listener {
	protected static final double DROP_TELEPORT_HEIGHT_OFFSET = 0.8;
	protected static final Vector UPWARD_VELOCITY = new Vector(0, .1, 0);
	
	@EventHandler
	public void ItemStackSpawn(final ItemSpawnEvent e) {
		if (!Oneblock.settings().droptossup) return;
		World world = Oneblock.getWorld();
		if (world == null) return;
        
		Entity drop = e.getEntity();
		Location loc = drop.getLocation();
		
		if (!world.equals(loc.getWorld())) return;
		if (loc.getBlockY() != Oneblock.getY()) return;
		if ((Oneblock.getX() - loc.getBlockX()) % Oneblock.getOffset() != 0) return;
		if ((Oneblock.getZ() - loc.getBlockZ()) % Oneblock.getOffset() != 0) return;
		
		loc.add(0, DROP_TELEPORT_HEIGHT_OFFSET, 0);

		// 1.21+ reworked item spawning so the old `teleport` path drops silently
		// lose their Z-axis velocity. Use the new copy() + setVelocity() API
		// on modern servers and the legacy teleport path on older ones.
		if (Oneblock.needDropFix) {
			e.setCancelled(true);
			drop.copy(loc).setVelocity(UPWARD_VELOCITY);
		} else {
			drop.teleport(loc);
			drop.setVelocity(UPWARD_VELOCITY);
		}
    }
	
	@EventHandler
	public void BlockBreak(final BlockBreakEvent e) {
		World world = Oneblock.getWorld();
		if (world == null) return;
		final Block block = e.getBlock();
		if (block.getWorld() != world) return;
		if (block.getY() != Oneblock.getY()) return;
		final Player ponl = e.getPlayer();
		final UUID uuid = ponl.getUniqueId();
		final int plID = PlayerInfo.getId(uuid);
		if (plID == -1) return;
		final int result[] = Oneblock.plugin.getIslandCoordinates(plID);
		if (block.getX() != result[0]) return;
		if (block.getZ() != result[1]) return;
		
		Bukkit.getScheduler().runTaskLater(Oneblock.plugin, () -> {Oneblock.plugin.generateBlock(result[0], result[1], plID, ponl, block);}, 1L);
	}
}