package AUR.util.knd;

import java.awt.Polygon;
import java.util.ArrayList;
import AUR.util.FibonacciHeap.Entry;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Set;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class AURAreaGraph {
	
	public Area area = null;
	public ArrayList<AURBorder> borders = new ArrayList<>();
	public ArrayList<AURAreaGraph> neighbours = new ArrayList<>();
	public AURWorldGraph wsg = null;
	public AURAreaGrid instanceAreaGrid = null;
	public final static int AREA_TYPE_ROAD = 0;
	public final static int AREA_TYPE_BULDING = 1;
	public final static int AREA_TYPE_REFUGE = 2;
	public final static int AREA_TYPE_ROAD_HYDRANT = 3;
	public final static int AREA_TYPE_GAS_STATION = 4;
	public int updateTime = -1;
	public AURNode lastDijkstraEntranceNode = null;
	public AURNode lastNoBlockadeDijkstraEntranceNode = null;
	public final static int COLOR_RED = 0;
	public final static int COLOR_GREEN = 1;
	public final static int COLOR_BLUE = 2;
	public final static int COLOR_YELLOW = 3;
	public int color = 0;
	public int clusterIndex = 0;
	public boolean vis;
	public boolean needUpdate;
	public boolean onFireProbability;
	private boolean seen;
	public boolean burnt;
	public boolean fireChecked;
	public int ownerAgent = -1;
	public Polygon polygon = null;
	public double goundArea = 0;
	public double perimeter = 0;
	
	public int getBlockadeForgetTime() {
		switch (wsg.ai.me().getStandardURN()) {
			case POLICE_FORCE: {
				return AURConstants.PathPlanning.POLICE_BLOCKADE_FORGET_TIME;
			}
			case AMBULANCE_TEAM: {
				return AURConstants.PathPlanning.AMBULANCE_BLOCKADE_FORGET_TIME;
			}
			case FIRE_BRIGADE: {
				return AURConstants.PathPlanning.FIREBRIGADE_BLOCKADE_FORGET_TIME;
			}
		}
		return AURConstants.PathPlanning.DEFAULT_BLOCKADE_FORGET_TIME;
	}
	
	private AURBuilding building = null;
	
	public ArrayList<AURBuilding> perceptibleAndExtinguishableBuildings;
	public ArrayList<AURBuilding> sightableBuildings;
	
	public void setSeen() {
		lastSeen = wsg.ai.getTime();
		this.seen = true;
	}
	
	public boolean seen() {
		return this.seen;
	}
	
	
	public int getX() {
		return this.area.getX();
	}
	
	public int getY() {
		return this.area.getY();
	}
	
	public int getTravelCost() {
		if(this.lastDijkstraEntranceNode == null) {
			return AURConstants.Math.INT_INF;
		}
		return this.lastDijkstraEntranceNode.cost;
	}
	
	public int getNoBlockadeTravelCost() {
		if(this.lastNoBlockadeDijkstraEntranceNode == null) {
			return AURConstants.Math.INT_INF;
		}
		return this.lastNoBlockadeDijkstraEntranceNode.cost;
	}

	public int getTravelTime() {
		if(this.lastDijkstraEntranceNode == null) {
			return AURConstants.Math.INT_INF;
		}
		return (int) (Math.ceil((double) this.getTravelCost() / AURConstants.Agent.VELOCITY));
	}
	
	public int getNoBlockadeTravelTime() {
		if(this.lastDijkstraEntranceNode == null) {
			return AURConstants.Math.INT_INF;
		}
		return (int) (Math.ceil((double) this.getNoBlockadeTravelCost() / AURConstants.Agent.VELOCITY));
	}
	

	public boolean isNeighbour(AURAreaGraph ag) {
		for (AURAreaGraph neiAg : neighbours) {
			if (neiAg.area.getID().equals(ag.area.getID())) {
				return true;
			}
		}
		return false;
	}

	public double distFromAgent() {
		return Math.hypot(this.getX() - wsg.ai.getX(), this.getY() - wsg.ai.getY());
	}

	public boolean isOnFire() {
		if (isBuilding() == false) {
			return false;
		}
		Building b = (Building) (this.area);
		if (b.isFierynessDefined() == false) {
			return false;
		}
		if (false || b.getFierynessEnum().equals(Fieryness.HEATING) || b.getFierynessEnum().equals(Fieryness.BURNING)
				|| b.getFierynessEnum().equals(Fieryness.INFERNO)) {
			return true;
		}
		return false;
	}

	public boolean damage() {
		if (isBuilding()) {
			Building b = (Building) area;
			if (b.isFierynessDefined()) {
				if (false || b.getFierynessEnum().equals(Fieryness.WATER_DAMAGE)
						|| b.getFierynessEnum().equals(Fieryness.MINOR_DAMAGE)
						|| b.getFierynessEnum().equals(Fieryness.MODERATE_DAMAGE)
						|| b.getFierynessEnum().equals(Fieryness.SEVERE_DAMAGE)) {
					return true;
				}
			}
		}
		return false;
	}

	public int distFromPointToBorder(double fx, double fy, AURBorder border) {
		return (int) AURGeoUtil.dist(fx, fy, border.CenterNode.x, border.CenterNode.y);
	}

	public double distFromBorderToBorder(AURBorder b1, AURBorder b2) {
		return AURGeoUtil.dist(b1.CenterNode.x, b1.CenterNode.y, b2.CenterNode.x, b2.CenterNode.y);
	}
	
//	public int countUnburntsInGrid() {
//		int result = 0;
//
//		int i = (int) ((this.getX() - wsg.gridDy) / wsg.worldGridSize);
//		int j = (int) ((this.getY() - wsg.gridDx) / wsg.worldGridSize);
//		if (wsg.areaGraphsGrid[i][j] != null) {
//
//			for(AURAreaGraph ag : wsg.areaGraphsGrid[i][j]) {
//				if(ag.isBuilding() && ag.burnt == false && ag.isOnFire()) {
//					result++;
//				}
//			}
//		}
//		
//		return result;
//	}
		
	public int getWaterNeeded() {
		if (isBuilding() == false) {
			return 0;
		}
		return getBuilding().fireSimBuilding.getWaterNeeded();
	}
	
	public boolean isSmall() {
		return this.goundArea < 1000 * 1000 * 25;
	}
	
	public boolean isBig() {
		return this.goundArea > (wsg.worldGridSize * wsg.worldGridSize * 4) / 6;
	} 
	
	public AURAreaGraph(Area area, AURWorldGraph wsg, AURAreaGrid instanceAreaGrid) {
		if (area == null || wsg == null) {
			return;
		}
		this.polygon = (Polygon) (area.getShape());
		this.goundArea = AURGeoUtil.getArea(this.polygon);
		this.perimeter = AURGeoUtil.getPerimeter(this.polygon);
		this.area = area;
		this.vis = false;
		this.wsg = wsg;
		this.instanceAreaGrid = instanceAreaGrid;

		if(isBuilding()) {
			this.building = new AURBuilding(this.wsg, this);
		}
	}
	
	public final AURBuilding getBuilding() {
                return this.building;
	}
        
	public final boolean isGasStation() {
		StandardEntityURN urn = this.area.getStandardURN();
		return (urn.equals(StandardEntityURN.GAS_STATION));
	}
	
	public final boolean isRoad() {
		StandardEntityURN urn = this.area.getStandardURN();
		return (urn.equals(StandardEntityURN.ROAD) || urn.equals(StandardEntityURN.HYDRANT));
	}
	
	public final boolean isHydrant() {
		StandardEntityURN urn = this.area.getStandardURN();
		return (urn.equals(StandardEntityURN.HYDRANT));
	}
	
	public final boolean isRefuge() {
		StandardEntityURN urn = this.area.getStandardURN();
		return (urn.equals(StandardEntityURN.REFUGE));
	}
	
	public final boolean isBuilding() {
		StandardEntityURN urn = this.area.getStandardURN();
		return (false
			|| urn.equals(StandardEntityURN.BUILDING)
			|| urn.equals(StandardEntityURN.GAS_STATION)
			|| urn.equals(StandardEntityURN.REFUGE)
			|| urn.equals(StandardEntityURN.POLICE_OFFICE)
			|| urn.equals(StandardEntityURN.AMBULANCE_CENTRE)
			|| urn.equals(StandardEntityURN.FIRE_STATION)
		);
	}

	public ArrayList<AURNode> getReachabeEdgeNodes(double x, double y) {
		ArrayList<AURNode> result = new ArrayList<>();
		if (area.getShape().contains(x, y) == false) {
			if (area.getShape().intersects(x - 10, y - 10, 20, 20) == false) {
				result.clear();
				return result;
			}
		}

		if (this.hasBlockade() == false) {
			for (AURBorder border : borders) {
				for (AURNode node : border.nodes) {
					node.cost = (int) AURGeoUtil.dist(x, y, node.x, node.y);
					result.add(node);
				}

			}
			return result;
		}
		result.addAll(instanceAreaGrid.getReachableEdgeNodesFrom(this, x, y));
		return result;
	}
	
	public ArrayList<AUREdgeToStand> getEdgesToPerceptiblePolygons(int x, int y) {
		ArrayList<AUREdgeToStand> result = new ArrayList<>();
		if (area.getShape().contains(x, y) == false) {
			if (area.getShape().intersects(x - 10, y - 10, 20, 20) == false) {
				result.clear();
				return result;
			}
		}
		return instanceAreaGrid.getEdgesToPerceptiblePolygons(this, x, y);
	}
	
	public ArrayList<AUREdgeToStand> getEdgesToSightPolygons(int x, int y) {
		ArrayList<AUREdgeToStand> result = new ArrayList<>();
		if (area.getShape().contains(x, y) == false) {
			if (area.getShape().intersects(x - 10, y - 10, 20, 20) == false) {
				result.clear();
				return result;
			}
		}
		return instanceAreaGrid.getEdgesToSightPolygon(this, x, y);
	}

	public ArrayList<AURNode> getEdgeToAllBorderCenters(double x, double y) {
		ArrayList<AURNode> result = new ArrayList<>();
		for (AURBorder border : borders) {
			border.CenterNode.cost = distFromPointToBorder(x, y, border);
			result.add(border.CenterNode);
		}
		return result;
	}

	public Entry<AURAreaGraph> pQueEntry = null;

	public double lineDistToClosestGasStation() {
		double minDist = AURGeoUtil.INF;
		double dist = 0;
		for (AURAreaGraph ag : wsg.gasStations) {
			Building b = (Building) (ag.area);
			if (b.isFierynessDefined() == false || b.getFierynessEnum().equals(Fieryness.UNBURNT)) {
				dist = AURGeoUtil.dist(ag.getX(), ag.getY(), this.getX(), this.getY());
				if (dist < minDist) {
					minDist = dist;
				}
			}
		}
		return minDist;
	}

	public ArrayList<AURBuilding> getCloseBuildings() {
		Rectangle2D bounds = this.polygon.getBounds();
		int a = AURConstants.Misc.CLOSE_BUILDING_THRESHOLD;
		Collection<StandardEntity> cands = wsg.wi.getObjectsInRectangle(
			(int) bounds.getMinX() - a,
			(int) bounds.getMinY() - a,
			(int) bounds.getMaxX() + a,
			(int) bounds.getMaxY() + a
		);
		ArrayList<AURBuilding> result = new ArrayList<>();
		for(StandardEntity sent : cands) {
			if(AURUtil.isBuilding(sent)) {
				AURAreaGraph ag_ = this.wsg.getAreaGraph(sent.getID());
				if(ag_ != null && ag_.isBuilding()) {
					result.add(ag_.getBuilding());
				}
			}
		}
		return result;
	}
	
	private int lastSeen = 0;

	public int noSeeTime() {
		return wsg.ai.getTime() - lastSeen;
	}

	public void update(AURWorldGraph wsg) {
		lastDijkstraEntranceNode = null;
		lastNoBlockadeDijkstraEntranceNode = null;
		pQueEntry = null;
		this.needUpdate = false;
		if (wsg.changes.contains(area.getID()) || updateTime < 0) {
			updateTime = wsg.ai.getTime();
			this.needUpdate = true;
		}
		this.needUpdate = this.needUpdate || (longTimeNoSee() && hasBlockade());
		if (this.needUpdate) {
			for (AURBorder border : borders) {
				border.reset();
			}
		}
		
		if(isBuilding()) {
			int temp = 0;
			Building b = ((Building) (this.area));
			temp = 0;
			if(b.isTemperatureDefined()) {
				temp = b.getTemperature();
			}
			if(isOnFire()) {
				if(fireReportTime == -1 || temp != lastTemperature) {
					fireReportTime = this.wsg.ai.getTime();
				}
			} else {
				this.fireReportTime = -1;
			}
			lastTemperature = temp;
		}
		if(this.isBuilding() == true) {
			this.getBuilding().update();
		}
	}

//	public ArrayList<Polygon> getBlockades() {
//		ArrayList<Polygon>  result = new ArrayList<>();
//		if(this.area.isBlockadesDefined() == false) {
//			return result;
//		}
//		for (EntityID entId : this.area.getBlockades()) {
//			Blockade b = (Blockade) wsg.wi.getEntity(entId);
//			result.add((Polygon) (b.getShape()));
//		}
//		return result;
//	}
	
	public ArrayList<Polygon> getAliveBlockades() {
		ArrayList<Polygon>  result = new ArrayList<>();
		if(longTimeNoSee()) {
			return result;
		}
		if(this.area.isBlockadesDefined() == false) {
			return result;
		}
		for (EntityID entId : this.area.getBlockades()) {
			Blockade b = (Blockade) wsg.wi.getEntity(entId);
			result.add((Polygon) (b.getShape()));
		}
		return result;
	}
	
	public boolean hasBlockade() {
		if(this.area.isBlockadesDefined() == false) {
			return false;
		}
		return this.area.getBlockades().isEmpty() == false;
	}
	
	public int fireReportTime = -1;
	public int lastTemperature = 0;
	
	public final static int FIRE_REPORT_FORGET_TIME = 6;
	
	public boolean isRecentlyReportedFire() {
		return (wsg.ai.getTime() - fireReportTime) <= FIRE_REPORT_FORGET_TIME;
	}
	
	public void initForReCalc() {
		this.needUpdate = true;
	}

	public void addBorderCenterEdges() {
		AURBorder iB;
		AURBorder jB;
		double cost;
		AUREdge edge = null;
		for (int i = 0; i < borders.size(); i++) {
			iB = borders.get(i);
			for (int j = i + 1; j < borders.size(); j++) {
				jB = borders.get(j);
				cost = distFromBorderToBorder(iB, jB);
				edge = new AUREdge(iB.CenterNode, jB.CenterNode, (int) cost, this);
				iB.CenterNode.edges.add(edge);
				jB.CenterNode.edges.add(edge);

			}
		}
	}
	
	public double getScore() {
		double perceptScore = 0;
		int p = 1;
		if(perceptibleAndExtinguishableBuildings != null) {
			perceptScore = (double) Math.pow(perceptibleAndExtinguishableBuildings.size(), p) / Math.pow(wsg.getMaxPerceptibleBuildings(), p);
		}
		
		double aScore = 1 - (Math.pow(AURGeoUtil.getArea((Polygon) area.getShape()), p) / Math.pow(wsg.getMaxRoadArea(), p));
		
		double perimeterScore = 1 - (Math.pow(AURGeoUtil.getPerimeter((Polygon) area.getShape()), p) / Math.pow(wsg.getMaxRoadPerimeter(), p));
		
		
		//pScore = Math.pow(pScore, 0.1);
		double score = 1.0 * perceptScore * perimeterScore;
		
		return score;
	}

	public boolean longTimeNoSee() {
		return (wsg.ai.getTime() - updateTime) > getBlockadeForgetTime();
	}
	
	public void paint(Graphics2D g2, K_ScreenTransform kst) {

//		int a = 500;
//		for(AURBorder border : borders) {
//			for(AURNode node : border.nodes) {
//				g2.draw(kst.getTransformedRectangle(node.x - a, node.y - a, a * 2, a * 2));
//			}
//		}

	}
	
        
        // Added by Amir Aslan Aslani - Mar 2018
        public double baseScore = 1;
        public double secondaryScore = 1;
        
        public double getFinalScore(){
                return baseScore * secondaryScore;
        }
}
