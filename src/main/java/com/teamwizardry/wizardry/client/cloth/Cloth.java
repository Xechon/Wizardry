package com.teamwizardry.wizardry.client.cloth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.teamwizardry.librarianlib.features.math.Matrix4;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Cloth {

	public PointMass3D[][] masses;
	public Vec3d[] top;
	public Vec3d size;
	private List<Link3D> links = new ArrayList<>();
	private List<Link3D> hardLinks = new ArrayList<>();
	private int solvePasses = 5;
	private int height;
	private float stretch = 1, shear = 1, flex = 1.0f, air = 1.5f;
	private Vec3d gravity = new Vec3d(0, -0.01, 0);
	private Map<PointMass3D, Vec3d> relativePositions = new HashMap<>();

	public Cloth(Vec3d[] top, int height, Vec3d size) {
		this.top = top;
		this.height = height;
		this.size = size;
		stretch = 0.8f;
		shear = 0.8f;
		flex = 0.9f;
		init();
	}

	public void updateRelative(Vec3d pos, Vec3d rotation) {
		Matrix4 matrix = new Matrix4();
		matrix.translate(pos);
		matrix.rotate(Math.toRadians(rotation.x), new Vec3d(1, 0, 0));
		matrix.rotate(Math.toRadians(rotation.y), new Vec3d(0, 1, 0));
		matrix.rotate(Math.toRadians(rotation.z), new Vec3d(0, 0, 1));
		for (Entry<PointMass3D, Vec3d> entry : relativePositions.entrySet()) {
			Vec3d trans = matrix.apply(entry.getValue());
			entry.getKey().origPos = entry.getKey().pos;
			entry.getKey().pos = trans;
		}
	}

	public void init() {
		masses = new PointMass3D[height][top.length];
		links = new ArrayList<>();

		// loop through grid to set masses
		for (int i = 0; i < height; i++)
			for (int j = 0; j < top.length; j++) {
				masses[i][j] = new PointMass3D(top[j].add(size.scale(i)), 0.1f); // get x (top[j], then add y (size.scale(i))
				if (i == 0) masses[0][j].pin = true; // pin it if it's on top.
			}

		for (int x = 0; x < masses.length; x++) {
			for (int z = 0; z < masses[x].length; z++) {

				// set all the top row's links to immovable ones so it doesn't look like it's held by cloth pins
				if ((x + 1) < masses.length)
					hardLinks.add(new HardLink3D(masses[x][z], masses[x + 1][z], 1));

				// set a link on the x-axis to the right of the point
				if ((x + 1) < masses.length)
					links.add(new Link3D(masses[x][z], masses[x + 1][z], stretch / solvePasses));
				// set a link on the z-axis to under the point
				if (((z + 1) < masses[x].length) && (x != 0))
					links.add(new Link3D(masses[x][z], masses[x][z + 1], stretch / solvePasses));

				// set a link diagonally downwards right
				if (((x + 1) < masses.length) && ((z + 1) < masses[x].length))
					links.add(new Link3D(masses[x][z], masses[x + 1][z + 1], shear / solvePasses));
				// set a link diagonally downwards left
				if (((x + 1) < masses.length) && ((z - 1) >= 0))
					links.add(new Link3D(masses[x][z], masses[x + 1][z - 1], shear / solvePasses));
			}
		}

		for (int x = 0; x < masses.length; x++) {
			for (int z = 0; z < masses[x].length; z++) {
				if ((x + 2) < masses.length) {
					Vec3d pos1 = masses[x][z].pos, pos2 = masses[x + 1][z].pos, pos3 = masses[x + 2][z].pos;
					if ((pos1 != null) && (pos2 != null) && (pos3 != null)) {
						float dist = (float) (pos1.subtract(pos2).lengthVector() + pos2.subtract(pos3).lengthVector());
						links.add(new Link3D(masses[x][z], masses[x + 2][z], dist, flex / solvePasses));
					}
				}
				if ((z + 2) < masses[x].length) {
					Vec3d pos1 = masses[x][z].pos, pos2 = masses[x][z + 1].pos, pos3 = masses[x][z + 2].pos;
					if ((pos1 != null) && (pos2 != null) && (pos3 != null)) {
						float dist = (float) (pos1.subtract(pos2).lengthVector() + pos2.subtract(pos3).lengthVector());
						links.add(new Link3D(masses[x][z], masses[x][z + 2], dist, flex / solvePasses));
					}
				}

				if (((x + 2) < masses.length) && ((z + 2) < masses[x].length)) {
					Vec3d pos1 = masses[x][z].pos, pos2 = masses[x + 1][z + 1].pos, pos3 = masses[x + 2][z + 2].pos;
					if ((pos1 != null) && (pos2 != null) && (pos3 != null)) {
						float dist = (float) (pos1.subtract(pos2).lengthVector() + pos2.subtract(pos3).lengthVector());
						links.add(new Link3D(masses[x][z], masses[x + 2][z + 2], dist, flex / solvePasses));
					}
				}
				if (((x + 2) < masses.length) && ((z - 2) > 0)) {
					Vec3d pos1 = masses[x][z].pos, pos2 = masses[x + 1][z - 1].pos, pos3 = masses[x + 2][z - 2].pos;
					if ((pos1 != null) && (pos2 != null) && (pos3 != null)) {
						float dist = (float) (pos1.subtract(pos2).lengthVector() + pos2.subtract(pos3).lengthVector());
						links.add(new Link3D(masses[x][z], masses[x + 2][z - 2], dist, flex / solvePasses));
					}
				}
			}
		}

	}

	/**
	 * Calls {@link #pushOutPoint(PointMass3D, List, List)} for all the points in the mesh
	 *
	 * @param aabbs
	 * @param spheres
	 */
	private void pushOutPoints(List<AxisAlignedBB> aabbs, List<Sphere> spheres) {
		for (PointMass3D[] column : masses) {
			for (PointMass3D point : column) {
				pushOutPoint(point, aabbs, spheres);
			}
		}
	}

	/**
	 * Pushes the point out of the passed AABBs and Boxes
	 *
	 * @param point
	 * @param aabbs
	 * @param spheres
	 */
	private void pushOutPoint(PointMass3D point, List<AxisAlignedBB> aabbs, List<Sphere> spheres) {
		if (point.pin) return;
		for (Sphere sphere : spheres) {
			point.pos = sphere.fix(point.pos);
		}
	}

	/**
	 * Calls {@link #collidePoint(PointMass3D, List, List)} to all the points in the mesh
	 *
	 * @param aabbs
	 * @param spheres
	 */
	private void collidePoints(List<AxisAlignedBB> aabbs, List<Sphere> spheres) {
		for (PointMass3D[] column : masses) {
			for (PointMass3D point : column) {
				collidePoint(point, aabbs, spheres);
			}
		}
	}

	/**
	 * Applies motion collision for the passed point using the passed AABBs and Boxes.
	 *
	 * @param point
	 * @param aabbs
	 * @param spheres
	 */
	private void collidePoint(PointMass3D point, List<AxisAlignedBB> aabbs, List<Sphere> spheres) {
		if (point.pin)
			return;
		point.friction = null;
		for (AxisAlignedBB aabb : aabbs) {
			Vec3d res = calculateIntercept(aabb, point, true);
			if (res != null) {
				point.pos = res;
			}
		}
		for (AxisAlignedBB aabb : aabbs) {
			Vec3d res = calculateIntercept(aabb, point, false);
			if (res != null) {
				point.pos = res;
			}
		}
		for (Sphere sphere : spheres) {
			Vec3d res = sphere.trace(point.origPos, point.pos);
			if (res != null) {
				point.pos = res;
			}
		}
		double friction = 0.2;
		point.applyMotion((point.friction == null) ? Vec3d.ZERO : point.friction.scale(-friction));
	}

	private void applyMotionToPoints() {
		for (int x = 0; x < masses.length; x++) {
			for (int y = 0; y < masses[x].length; y++) {
				applyMotionToPoint(x, y, masses[x][y]);
			}
		}
	}

	private void applyMotionToPoint(int x, int y, PointMass3D point) {
		if (point.pin) return;
		if ((point.pos == null) || (point.prevPos == null)) return;

		Vec3d lastMotion = point.pos.subtract(point.prevPos);
		point.applyMotion(lastMotion); // existing motion
		point.applyForce(gravity); // gravity

		Vec3d wind = new Vec3d(0.0, 0.0, 1.0 / 20.0).subtract(lastMotion);
//		wind = Vec3d.ZERO;
		Vec3d normal = Vec3d.ZERO;

		if ((x > 0) && (y > 0)) {
			normal = normal.add(Geometry.getNormal(point.origPos, masses[x][y - 1].origPos, masses[x - 1][y].origPos));

		}

		if ((x > 0) && ((y + 1) < masses[x].length)) {
			normal = normal.add(Geometry.getNormal(point.origPos, masses[x][y + 1].origPos, masses[x - 1][y].origPos));
		}

		if (((x + 1) < masses.length) && ((y + 1) < masses[x].length)) {
			normal = normal.add(Geometry.getNormal(point.origPos, masses[x][y + 1].origPos, masses[x + 1][y].origPos));
		}

		if (((x + 1) < masses.length) && (y > 0)) {
			normal = normal.add(Geometry.getNormal(point.origPos, masses[x][y - 1].origPos, masses[x + 1][y].origPos));
		}

		normal = normal.normalize();
		Vec3d windNormal = wind.normalize();

		double angle = StrictMath.acos(MathUtil.clamp(windNormal.dotProduct(normal), -1, 1));
		if (angle > (Math.PI / 2))
			normal = normal.scale(-1);

		// https://books.google.com/books?id=x5cLAQAAIAAJ&pg=PA5&lpg=PA5&dq=wind+pressure+on+a+flat+angled+surface&source=bl&ots=g090hiOfxv&sig=MqZQhLMozsMNndJtkA1R_bk5KiA&hl=en&sa=X&ved=0ahUKEwiozMW2z_vNAhUD7yYKHeqvBVcQ6AEILjAC#v=onepage&q&f=false
		// component 5-6. I'm using formula (5)
		// wind vector length squared is flat pressure. All the other terms can
		// be changed in the air coefficent.
		Vec3d force = normal.add(windNormal).normalize().scale((StrictMath.pow(wind.lengthVector(), 2) * angle) / (Math.PI / 4));

		point.applyForce(force.scale(air));

		point.friction = null;
	}

	private List<AxisAlignedBB> getAABBs(Entity e) {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

		for (PointMass3D[] mass1 : masses) {
			for (PointMass3D mass : mass1) {
				if (mass.origPos == null)
					mass.origPos = mass.pos;
				if (mass.pos != null) {
					minX = Math.min(minX, Math.min(mass.pos.x, mass.origPos.x));
					minY = Math.min(minY, Math.min(mass.pos.y, mass.origPos.y));
					minZ = Math.min(minZ, Math.min(mass.pos.z, mass.origPos.z));
					if ((maxX - minX) > 10)
						minX = maxX - 10;
					if ((maxY - minY) > 10)
						minY = maxY - 10;
					if ((maxZ - minZ) > 10)
						minZ = maxZ - 10;

					maxX = Math.max(maxX, Math.max(mass.pos.x, mass.origPos.x));
					maxY = Math.max(maxY, Math.max(mass.pos.y, mass.origPos.y));
					maxZ = Math.max(maxZ, Math.max(mass.pos.z, mass.origPos.z));

					if ((maxX - minX) > 10)
						maxX = minX + 10;
					if ((maxY - minY) > 10)
						maxY = minY + 10;
					if ((maxZ - minZ) > 10)
						maxZ = minZ + 10;
				}
			}
		}
		double m = 0.5;
		AxisAlignedBB checkAABB = new AxisAlignedBB(minX - m, minY - m, minZ - m, maxX + m, maxY + m, maxZ + m);

		return e.world.getCollisionBoxes(e, checkAABB);
	}

	public void tick(Entity e, List<Sphere> spheres) {

		List<AxisAlignedBB> aabbs = getAABBs(e);
		pushOutPoints(aabbs, spheres);

		for (PointMass3D[] column : masses) {
			for (PointMass3D point : column) {
				if (!point.pin) {
					point.prevPos = point.pos;
					point.origPos = point.pos;
				}
			}
		}

		applyMotionToPoints();

		aabbs = getAABBs(e);

		collidePoints(aabbs, spheres);
		pushOutPoints(aabbs, spheres);

		for (int i = 0; i < solvePasses; i++) {
			links.forEach(Link3D::resolve);
			hardLinks.forEach(Link3D::resolve);
			collidePoints(ImmutableList.of(), spheres);
		}

		collidePoints(aabbs, spheres);
		pushOutPoints(aabbs, spheres);

		hardLinks.stream().filter(link -> (link.a.pos != null) && (link.b.pos != null)).forEach(link -> {
			if ((link.a.pos != null) && (link.b.pos != null)) {
				Vec3d posDiff = link.a.pos.subtract(link.b.pos);
				double d = posDiff.lengthVector();

				double difference = d - link.distance;
				if (difference > link.distance)
					link.resolve();
			}
		});
	}

	@Nullable
	private Vec3d calculateIntercept(AxisAlignedBB aabb, PointMass3D point, boolean yOnly) {
		Vec3d vecA = point.origPos, vecB = point.pos;

		Vec3d vecY = null;

		if ((vecA != null) && (vecB != null)) {
			if (vecA.y > vecB.y) {
				vecY = collideWithYPlane(aabb, aabb.maxY, vecA, vecB);
			}

			if (vecA.y < vecB.y) {
				vecY = collideWithYPlane(aabb, aabb.minY, vecA, vecB);
			}
		}

		if (vecY != null) {
			point.friction = new Vec3d(vecB.x - vecY.x, 0, vecB.z - vecY.z);
			return new Vec3d(vecB.x, vecY.y, vecB.z);
		}

		if (yOnly)
			return null;

		Vec3d vecX = null;
		if ((vecA != null) && (vecB != null)) {
			if (vecA.x > vecB.x) {
				vecX = collideWithXPlane(aabb, aabb.maxX, vecA, vecB);
			}

			if (vecA.x < vecB.x) {
				vecX = collideWithXPlane(aabb, aabb.minX, vecA, vecB);
			}
		}

		if (vecX != null) {
			point.friction = new Vec3d(0, vecB.y - vecX.y, vecB.z - vecX.z);
			return new Vec3d(vecX.x, vecB.y, vecB.z);
		}

		Vec3d vecZ = null;
		if ((vecA != null) && (vecB != null)) {
			if (vecA.z > vecB.z) {
				vecZ = collideWithZPlane(aabb, aabb.maxZ, vecA, vecB);
			}

			if (vecA.z < vecB.z) {
				vecZ = collideWithZPlane(aabb, aabb.minZ, vecA, vecB);
			}
		}
		if (vecZ != null) {
			point.friction = new Vec3d(vecB.x - vecZ.x, vecB.y - vecZ.y, 0);
			return new Vec3d(vecB.x, vecB.y, vecZ.z);
		}

		return null;
	}

	@Nullable
	@VisibleForTesting
	private Vec3d collideWithXPlane(AxisAlignedBB aabb, double p_186671_1_, Vec3d p_186671_3_, Vec3d p_186671_4_) {
		Vec3d vec3d = p_186671_3_.getIntermediateWithXValue(p_186671_4_, p_186671_1_);
		return ((vec3d != null) && intersectsWithYZ(aabb, vec3d)) ? vec3d : null;
	}

	@Nullable
	@VisibleForTesting
	private Vec3d collideWithYPlane(AxisAlignedBB aabb, double p_186663_1_, Vec3d p_186663_3_, Vec3d p_186663_4_) {
		Vec3d vec3d = p_186663_3_.getIntermediateWithYValue(p_186663_4_, p_186663_1_);
		return ((vec3d != null) && intersectsWithXZ(aabb, vec3d)) ? vec3d : null;
	}

	@Nullable
	@VisibleForTesting
	private Vec3d collideWithZPlane(AxisAlignedBB aabb, double p_186665_1_, Vec3d p_186665_3_, Vec3d p_186665_4_) {
		Vec3d vec3d = p_186665_3_.getIntermediateWithZValue(p_186665_4_, p_186665_1_);
		return ((vec3d != null) && intersectsWithXY(aabb, vec3d)) ? vec3d : null;
	}

	@VisibleForTesting
	private boolean intersectsWithYZ(AxisAlignedBB aabb, Vec3d vec) {
		double m = -0.0;
		return (vec.y > (aabb.minY + m)) && (vec.y < (aabb.maxY - m)) && (vec.z > (aabb.minZ + m)) && (vec.z < (aabb.maxZ - m));
	}

	@VisibleForTesting
	private boolean intersectsWithXZ(AxisAlignedBB aabb, Vec3d vec) {
		double m = -0.0;
		return (vec.x > (aabb.minX + m)) && (vec.x < (aabb.maxX - m)) && (vec.z > (aabb.minZ + m)) && (vec.z < (aabb.maxZ - m));
	}

	@VisibleForTesting
	private boolean intersectsWithXY(AxisAlignedBB aabb, Vec3d vec) {
		return (vec.x > aabb.minX) && (vec.x < aabb.maxX) && (vec.y > aabb.minY) && (vec.y < aabb.maxY);
	}

}