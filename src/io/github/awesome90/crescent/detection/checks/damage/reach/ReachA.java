package io.github.awesome90.crescent.detection.checks.damage.reach;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import io.github.awesome90.crescent.Crescent;
import io.github.awesome90.crescent.detection.checks.Check;
import io.github.awesome90.crescent.detection.checks.CheckVersion;

public class ReachA extends CheckVersion {

	private static final double ALLOWED_REACH = Crescent.getInstance().getConfig().getDouble("reach.a.allowedReach");
	private static final double ALLOWED_REACH_SQUARED = ALLOWED_REACH * ALLOWED_REACH;
	private static final double ALLOWED_REACH_DIFFERENCE = Crescent.getInstance().getConfig()
			.getDouble("reach.a.allowedReachDifference");
	/**
	 * The amount of hits that need to be made in order for reach data to be
	 * considered.
	 */
	private static final int VALIDATION = Crescent.getInstance().getConfig().getInt("reach.a.reachDataValidation");

	/**
	 * The previous amounts of reach that a player has got.
	 */
	private ArrayList<Double> previousReach;

	public ReachA(Check check) {
		super(check, "A", "Checks the distance between the player and their target on attack.");
		this.previousReach = new ArrayList<Double>();
	}

	@Override
	public void call(Event event) {
		if (event instanceof EntityDamageByEntityEvent) {
			final EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;

			final Player player = profile.getPlayer();

			final double reachSquared = getDistanceSquaredXZ(player.getLocation(), edbe.getEntity().getLocation());

			previousReach.add(reachSquared);

			// How far the player's reach is from the allowed value.
			final double reachOffset = reachSquared - ALLOWED_REACH_SQUARED;

			if (reachOffset > ALLOWED_REACH_DIFFERENCE) {
				callback(true);
			} else {
				// Player's reach is slightly over the allowed amount.

				// If enough data has been collected.
				if (previousReach.size() >= VALIDATION) {
					if (getStrangeReachPercentage() > 50.0) {
						// More than half of the player's hits have been
						// suspicious. This can't be right.
						callback(true);
					}
				}
			}
		}
	}

	@Override
	public double checkCurrentCertainty() {
		return getStrangeReachPercentage();
	}

	private double getDistanceSquaredXZ(Location from, Location to) {
		final double diffX = Math.abs(from.getX() - to.getX()), diffY = Math.abs(from.getY() - to.getY());
		return diffX * diffX + diffY * diffY;
	}

	/**
	 * @return The percentage of all hits which had a larger than allowed reach.
	 */
	private double getStrangeReachPercentage() {
		double totalReach = 0.0;
		double totalOffset = 0.0;

		for (double reach : previousReach) {
			totalReach += reach;

			if (reach > ALLOWED_REACH_SQUARED) {
				double difference = reach - ALLOWED_REACH_SQUARED;

				if (difference <= ALLOWED_REACH_DIFFERENCE) {
					// They are less likely to be cheating.
					difference /= ALLOWED_REACH_DIFFERENCE * 100.0;
				}

				totalOffset += difference;
			}
		}

		if (totalReach == 0.0) {
			// We can't divide by zero!
			return 0.0;
		}

		return (totalOffset / totalReach) * 100.0;
	}

}