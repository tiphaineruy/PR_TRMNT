import com.sun.jmx.remote.internal.ArrayQueue;

import java.util.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 */
class Player {

    public static int myId = -1;

    public static void main(String args[]) {

        // INIT //////////////////////////////////////////////////

        World world = new World();
        MovementManager moveManager = new MovementManager(world);
        DropManager dropManager = new DropManager(world);

        Scanner in = new Scanner(System.in);
        world.playerCount = in.nextInt(); // the amount of players (2 to 4)
        world.setMyId(Zone.myId = myId = in.nextInt()); // my player ID (0, 1, 2 or 3)
        world.setZoneCount(in.nextInt());// the amount of zones on the map
        world.setLinkCount(in.nextInt()); // the amount of links between all zones

        world.initWorld(in);


        // GAME LOOP /////////////////////////////////////////
        while (true) {
/*            Date date1 = new Date();
            Date date2 = new Date();*/
            world.updateZones(in);
/*            Integer[] income = new Integer[]{0, 0, 0, 0};
            for (Zone zone : zones) {
                if (zone.getOwner() >= 0) income[zone.getOwner()] += zone.getPlatinum();
            }
            System.err.println("INCOME me:" + myId + "  // P0 " + income[0] + " P1 " + income[1] + " P2 " + income[2] + " P3 " + income[3]);*/

            world.secureAndDefend();
            world.generateOrderPath();

            // MOVE /////////////////////////////////////////////
            System.out.println(moveManager.update().out());
            // DROP ////////////////////////////////////////////
            System.out.println(dropManager.update().out());
//            date2 = new Date(); System.err.println("TIME:"+(date2.getTime()-date1.getTime()));
        }
    }

}

class DropManager {
    private World world;

    DropManager(World world) {
        this.world = world;
    }

    Map<Integer, Integer> drops = new HashMap<Integer, Integer>();

    public DropManager update() {
        drops = new HashMap<Integer, Integer>();
        // BUY ////////////////////////////////////////////////
        List<Zone> dropTo = new ArrayList<Zone>();
        dropTo.addAll(Zone.getNeutralZones(world.getPlatinumZones(), null));

        List<Zone> mines = Zone.getOwnedZones(world.getZones(), world.getMyId(), false);
        Collections.sort(mines, Zone.valueComparator);
        dropTo.addAll(mines);

        // defensive drop
        Collections.sort(world.getUndefended(), Zone.rawPlatinumComparator);
        if (world.getUndefended().size() > 0) {
            while (!world.getUndefended().isEmpty() && world.me.maxPods > 0) {
                Zone drop = world.getUndefended().get(0);

                if (drop.getDefend() > 0) { // DEFEND with in drop token
                    if (drop.getMoveAblePods() > 0) {
                        if (drop.getDefend() > 0) {
                            int defended = Math.min(drop.getDefend(), drop.getMoveAblePods());
                            drop.setDefend(drop.getDefend() - defended);
                            drop.setMoveAblePods(drop.getMoveAblePods() - defended);
                        }

                    }
                }

                if (drop.getDefend() > 0) {
                    world.me.maxPods = Movement.addDrop(drop, 1, drops, world.me.maxPods, world.getUndefended());
                }
                world.getUndefended().remove(drop);
            }
        }

        if (!dropTo.isEmpty()) {
            //buy
            Integer zoneToDropIndex = 0;
            while (world.me.maxPods > 0) {
                world.me.maxPods = Movement.addDrop(dropTo.get(zoneToDropIndex), (new Double(dropTo.get(zoneToDropIndex).getPlatinum() / 2)).intValue() + 1, drops, world.me.maxPods, world.getUndefended());
                zoneToDropIndex++;
                if (zoneToDropIndex >= dropTo.size()) zoneToDropIndex = 0;
            }
        }
        return this;
    }

    public String out() {
        //  OUTPUT
        if (drops.isEmpty()) {
            return "WAIT";
        } else {
            StringBuilder buyOut = new StringBuilder();
//                System.err.println("DROPS:"+drops);
            for (Integer zoneId : drops.keySet()) {
                buyOut.append(drops.get(zoneId)).append(" ").append(zoneId).append(" ");
            }
            return buyOut.toString().trim();
        }
    }
}

class MovementManager {
    private World world;

    MovementManager(World world) {
        this.world = world;
    }

    Map<Movement, Integer> moves = new HashMap<Movement, Integer>();

    public MovementManager update() {
        moves = new HashMap<Movement, Integer>();
        for (Zone zone : world.me.ownedWithPods) { // for each zone where I have pods
            List<Zone> priorities;

            //building pods move command
            int movingPods = zone.getMoveAblePods();
            if (zone.getSecured()) { // THE ZONE IS SAFE
                priorities = buildOrderPathMoveListForZone(zone.getAdjacentZones());
                if (!priorities.isEmpty()) {
                    for (int i = 0; i < movingPods; i++) {
                        if (priorities.get(0).getOrderPath() == 0) {
                            List<Zone> border = new ArrayList<Zone>();
                            int solution = 0;
                            while (solution < priorities.size() && priorities.get(solution).getOrderPath() == 0) {
                                border.add(priorities.get(solution));
                                solution++;
                            }
                            Collections.sort(border, Zone.valueComparator);
                            Movement.addMove(zone, border.get(0), moves, world.getUndefended());
                        } else {
                            Movement.addMove(zone, priorities.get(0), moves, world.getUndefended());
                        }

                    }
                }
            } else {  // THE ZONE IS A BORDER
                priorities = zone.getAdjacentZones();
//                    System.err.println("BEFORE MOVE:"+zone);
                for (int i = 0; i < movingPods; i++) { // for each pod of the zone
                    Collections.sort(priorities, Zone.valueComparator);
//                        System.err.println(zone);
                    Movement.addMove(zone, priorities.get(0), moves, world.getUndefended());

                }
            }
        }
        return this;
    }

    public String out() {
        if (moves.isEmpty()) {
            return "WAIT";
        } else {
            StringBuilder moveOut = new StringBuilder();
            for (Movement movement : moves.keySet()) {
                moveOut.append(moves.get(movement)).append(" ").append(movement.getFrom()).append(" ").append(movement.getTo()).append(" ");
            }
            return moveOut.toString().trim();
        }
    }

    private List<Zone> buildOrderPathMoveListForZone(final List<Zone> zones) {
        List<Zone> priorityMoves = new ArrayList<Zone>();
        priorityMoves.addAll(zones);
        Collections.sort(priorityMoves, new Comparator<Zone>() {
            @Override
            public int compare(Zone zone1, Zone zone2) {
                return (zone1.getOrderPath() - zone2.getOrderPath());
            }
        });
        return priorityMoves;
    }
}

class PlayerInfo{
    public Integer id;
//    public Integer platinum = 0;
    public Integer maxPods = 0;
    public Integer income =0;
    public List<Zone> owned = new ArrayList<Zone>();
    public List<Zone> ownedWithPods = new ArrayList<Zone>();

    PlayerInfo(Integer id) {
        this.id = id;
    }

    public static List<PlayerInfo> buildPlayer(World world){
        List<PlayerInfo> players = new ArrayList<PlayerInfo>();
        for (int i = 0; i < world.playerCount ; i++) {
            PlayerInfo player = new PlayerInfo(i);
            players.add(player);
        }
        return players;
    }

    public static void resetPlayers(List<PlayerInfo> players){
        for (PlayerInfo player:players){
            player.owned = new ArrayList<Zone>();
            player.ownedWithPods = new ArrayList<Zone>();
        }
    }

}

class Continent {
    public Integer continentNumber;
    public List<Zone> zones = new ArrayList<Zone>();

    Continent(Integer continentNumber) {
        this.continentNumber = continentNumber;
    }

    public static List<Continent> buildContinents(World world) {
        List<Continent> continents = new ArrayList<Continent>();
        List<Zone> buildingTiles = new ArrayList<Zone>();
        buildingTiles.addAll(world.getZones());
        int continentNumber = 0;
        while (!buildingTiles.isEmpty()) {
            Continent currentContinent = new Continent(continentNumber);
            Zone current = buildingTiles.get(0);
            current.continent = continentNumber;
            currentContinent.zones.add(current);
            Boolean continentMapped = false;
            List<Zone> previousStep = new ArrayList<Zone>();
            previousStep.add(current);
            while (!continentMapped) {
                List<Zone> adj = new ArrayList<Zone>();
                continentMapped = true;
                for (Zone zone : previousStep) {
                    for (Zone adjZone : zone.getAdjacentZones()) {
                        if (adjZone.continent < 0) {
                            continentMapped = false;
                            adjZone.continent = continentNumber;
                        }
                        if (!adj.contains(adjZone)) adj.add(adjZone);
                    }
                }
                for (Zone zone : adj) {
                    if (!currentContinent.zones.contains(zone)) {
                        currentContinent.zones.add(zone);
                    }
                }
                previousStep = adj;

            }
            buildingTiles.removeAll(currentContinent.zones);
            continents.add(currentContinent);

            System.err.println("Cont number: " + continentNumber + " " + currentContinent.continentNumber + " /cont size: " + currentContinent.zones.size());
            continentNumber++;
        }

        System.err.println("CONTINENTS: " + continents.size());
        return continents;
    }
}

class Cluster {
    public Integer clusterId;
    public List<Zone> zones = new ArrayList<Zone>();

    Cluster(Integer clusterId) {
        this.clusterId = clusterId;
    }

    public static List<Cluster> buildClusters(World world) {
        List<Cluster> clusters = new ArrayList<Cluster>();
        List<Zone> buildingTiles = new ArrayList<Zone>();
        buildingTiles.addAll(world.getPlatinumZones());
        System.err.println("TILES:" + buildingTiles.size());
        int clusterNumber = 0;
        while (!buildingTiles.isEmpty()) {
            Cluster currentCluster = new Cluster(clusterNumber);
            Zone current = buildingTiles.get(0);
            current.cluster = clusterNumber;
            currentCluster.zones.add(current);
            Boolean clusterMapped = false;
            List<Zone> previousStep = new ArrayList<Zone>();
            previousStep.add(current);
            while (!clusterMapped) {
                List<Zone> adj = new ArrayList<Zone>();
                clusterMapped = true;
                for (Zone zone : previousStep) {
                    for (Zone adjZone : zone.getAdjacentZones()) {
                        if (adjZone.cluster < 0 && adjZone.getPlatinum() > 0) {
                            clusterMapped = false;
                            adjZone.cluster = clusterNumber;
                        }
                        if (!adj.contains(adjZone) && adjZone.getPlatinum() > 0) adj.add(adjZone);
                    }
                }
                for (Zone zone : adj) {
                    if (!currentCluster.zones.contains(zone)) {
                        currentCluster.zones.add(zone);
                    }
                }
                previousStep = adj;

            }
            buildingTiles.removeAll(currentCluster.zones);
            clusters.add(currentCluster);

            System.err.println(current.getId() + "CLUST: number: " + clusterNumber + " " + currentCluster.clusterId + " / size: " + currentCluster.zones.size());
            clusterNumber++;
        }

        System.err.println("CLUSTER SIZE: " + clusters.size());
        return clusters;
    }
}

class World {

    private Integer myId;
    public PlayerInfo me;
    public Integer playerCount;
    private Integer zoneCount;
    private Integer linkCount;

    public List<Continent> continents;
    public List<Cluster> clusters;
    public List<PlayerInfo> players;

    private List<Zone> platinumZones = new ArrayList<Zone>();

    private List<Zone> zones = new ArrayList<Zone>();

    public List<Zone> unsecured = new ArrayList<Zone>();
    public List<Zone> secured = new ArrayList<Zone>();
    public List<Zone> undefended = new ArrayList<Zone>(); // for the moment undefended mean zone has enemy && has platinum, do not defend no value cells

    //SET_ORDERING_PATH (trail to move lone pods to frontiers)
    public void generateOrderPath() {
        List<Zone> temp = new ArrayList<Zone>();
        temp.addAll(secured);
        int lvl = 0;
        while (!temp.isEmpty() && lvl < 50) {
            lvl += 1;
            Set<Zone> toRemove = new HashSet<Zone>();
            for (Zone zone : temp) {
                for (Zone adjacent : zone.getAdjacentZones()) {
                    if (adjacent.getOrderPath() == lvl - 1) {
                        toRemove.add(zone);
                        zone.setOrderPath(lvl);
                        break;
                    }
                }
            }
            temp.removeAll(toRemove);
        }
    }

    // SECURE AND DEFEND /////////////////////////////////////
    public void secureAndDefend() {
        undefended = new ArrayList<Zone>();

        for (Zone zone : Zone.getOwnedZones(zones, myId, null)) {
            zone.setSecured(true);
            zone.setDefend(0);
            boolean valuable = zone.getPlatinum() > 0;
            for (Zone adj : zone.getAdjacentZones()) {
                if (adj.getOwner() != myId) {
                    zone.setSecured(false);
                    zone.setOrderPath(0); // zone is border
                    if (adj.getOwner() != -1 && valuable) { // enemy && strategic value
                        zone.setDefend(Math.min(zone.getDefend() + adj.podsOnZone(), 4)); //TODO TOUCHY
                    }

                } else if (adj.getOwner() == myId) {
                    zone.setOwnedAdjacentZones(zone.getOwnedAdjacentZones() + 1);
                }
            }
            zone.setSurroundingEnnemies(zone.getDefend());

            if (zone.getSecured())
                secured.add(zone);
            else
                unsecured.add(zone);

            if (zone.getDefend() > 0) { // DEFEND with in zone token
                if (zone.getMoveAblePods() > 0) {
                    if (zone.getDefend() > 0) {
                        int defended = Math.min(zone.getDefend(), zone.getMoveAblePods());
                        zone.setDefend(zone.getDefend() - defended);
                        zone.setMoveAblePods(zone.getMoveAblePods() - defended);
                    }

                }
                if (zone.getDefend() > 0) undefended.add(zone);
            }


        }
    }

    public void updateZones(Scanner in) {
        Integer platinum = in.nextInt(); // my available Platinum
        me.maxPods = (new Double(platinum / 20)).intValue();
        PlayerInfo.resetPlayers(players);
        // UPDATE ZONES /////////////////////////////////////////

        in.nextLine();
        for (int i = 0; i < getZoneCount(); i++) {
            int zId = in.nextInt(); // this zone's ID
            int ownerId = in.nextInt(); // the player who owns this zone (-1 otherwise)
            int podsP0 = in.nextInt(); // player 0's PODs on this zone
            int podsP1 = in.nextInt(); // player 1's PODs on this zone
            int podsP2 = in.nextInt(); // player 2's PODs on this zone (always 0 for a two player game)
            int podsP3 = in.nextInt(); // player 3's PODs on this zone (always 0 for a two or three player game)
            // updating zones
            Zone zone = zones.get(zId);
            zone.setOwner(ownerId);
            if(ownerId >=0) players.get(ownerId).owned.add(zone);
            zone.getPods()[0] = podsP0;
            if (podsP0>0 && ownerId ==0) players.get(0).ownedWithPods.add(zone);
            zone.getPods()[1] = podsP1;
            if (podsP0>0 && ownerId ==1) players.get(1).ownedWithPods.add(zone);
            zone.getPods()[2] = podsP2;
            if (podsP0>0 && ownerId ==2) players.get(2).ownedWithPods.add(zone);
            zone.getPods()[3] = podsP3;
            if (podsP0>0 && ownerId ==3) players.get(3).ownedWithPods.add(zone);

            zone.setMoveAblePods(zone.getPods()[myId]);
            zone.setMovedTo(0);
            zone.setOwnedAdjacentZones(0);
            // reset ordering path
            zone.setOrderPath(-1);
            in.nextLine();
        }
    }

    public void initWorld(Scanner in) { // can be 1 second long.
        // FOR zoneCount -> CREATE ZONE
        in.nextLine();
        for (int i = 0; i < getZoneCount(); i++) {
            int zoneId = in.nextInt(); // this zone's ID (between 0 and zoneCount-1)
            int platinumSource = in.nextInt(); // the amount of Platinum this zone can provide per game turn
            zones.add(new Zone(zoneId, platinumSource));
            if (platinumSource > 0) platinumZones.add(zones.get(zoneId)); // added to the list of profitable zones
            in.nextLine();
        }

        // FOR linkCount -> CREATE LINK
        for (int i = 0; i < getLinkCount(); i++) {
            Zone zone1 = zones.get(in.nextInt());
            Zone zone2 = zones.get(in.nextInt());
            zone1.getAdjacentZones().add(zone2);
            zone2.getAdjacentZones().add(zone1);
            in.nextLine();
        }

        // general info Platinum density
        for (Zone zone : zones) {
            Integer value = 0;
            for (Zone adjacent : zone.getAdjacentZones())
                value += ((new Double(adjacent.getPlatinum() / 2).intValue()));
//                value += adjacent.getPlatinum(); // TODO TOUCHY

            zone.setAdjacentPlatinum(value);
        }

        // SORTING PLAT  allow for drop on low value + near clusters
        Collections.sort(platinumZones, Zone.rawPlatinumComparator);

        continents = Continent.buildContinents(this);
        clusters = Cluster.buildClusters(this);
        players = PlayerInfo.buildPlayer(this);
        me = players.get(myId);
    }

    public List<Zone> getUndefended() {
        return undefended;
    }

    public List<Zone> getPlatinumZones() {
        return platinumZones;
    }

    public Integer getMyId() {
        return myId;
    }

    public void setMyId(Integer myId) {
        this.myId = myId;
    }

    public Integer getZoneCount() {
        return zoneCount;
    }

    public void setZoneCount(Integer zoneCount) {
        this.zoneCount = zoneCount;
    }

    public Integer getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(Integer linkCount) {
        this.linkCount = linkCount;
    }

    public List<Zone> getZones() {
        return zones;
    }
}

class Movement {
    Integer from;
    Integer to;

    public Movement(Integer start, Integer to) {
        this.from = start;
        this.to = to;
    }

    public Integer getFrom() {
        return from;
    }

    public Integer getTo() {
        return to;
    }

    public static void addMove(Zone from, Zone to, Map<Movement, Integer> moves, List<Zone> toDefend) {
        if (from.getMoveAblePods() <= 0) return; // not enough pod to complete move
        from.setMoveAblePods(from.getMoveAblePods() - 1); // --podNumber
        to.setMovedTo(to.getMovedTo() + 1); // ++ movedTo
        if (to.getDefend() > 0) {
            to.setDefend(to.getDefend() - 1); // zone defended by +1 pods
            if (to.getDefend() > 0) toDefend.remove(to);
        }
        addMove(from.getId(), to.getId(), moves);
    }

    private static void addMove(Integer from, Integer to, Map<Movement, Integer> moves) {

        if (moves.get(new Movement(from, to)) != null) {
            moves.put(new Movement(from, to), moves.get(new Movement(from, to)) + 1);
        } else {
            moves.put(new Movement(from, to), 1);
        }
    }

    public static Integer addDrop(Zone location, Integer size, Map<Integer, Integer> drops, Integer maxDrop, List<Zone> toDefend) {
        int realDropSize = Math.min(size, maxDrop);
        if (drops.get(location.getId()) == null)
            drops.put(location.getId(), realDropSize);
        else
            drops.put(location.getId(), drops.get(location.getId()) + realDropSize);

        if (location.getDefend() > 0) {
            location.setDefend(location.getDefend() - realDropSize); // zone defended by +1 pods
            if (location.getDefend() > 0) toDefend.remove(location);
        }
        return maxDrop - realDropSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movement)) return false;

        Movement movement = (Movement) o;

        if (from != null ? !from.equals(movement.from) : movement.from != null) return false;
        if (to != null ? !to.equals(movement.to) : movement.to != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }
}

class Zone {
    public static Integer myId = -1;

    private List<Zone> adjacentZones = new ArrayList<Zone>();
    private int id;
    private Integer platinum = 0;
    private int owner = -1;
    private Integer[] pods = new Integer[]{0, 0, 0, 0};
    private Boolean secured = false;
    private Integer orderPath = -1; // if orderPath == -1 cell is not mine, if orderPath == 0 cell is frontier, elle cell is secured
    private Integer defend = 0; // pods needed to defend zone
    private Integer moveAblePods = 0; // pods available for move
    private Integer movedTo = 0; // number of pods that have moved this zone
    private Integer adjacentPlatinum = 0;
    private Integer ownedAdjacentZones = 0;

    public Integer continent = -1;
    public Integer cluster = -1;

/*    public Integer getSurroundingEnnemies() {
        return surroundingEnnemies;
    }*/

    public void setSurroundingEnnemies(Integer surroundingEnnemies) {
        this.surroundingEnnemies = surroundingEnnemies;
    }

    private Integer surroundingEnnemies = 0;

    public Integer getOwnedAdjacentZones() {
        return ownedAdjacentZones;
    }

    public void setOwnedAdjacentZones(Integer ownedAdjacentZones) {
        this.ownedAdjacentZones = ownedAdjacentZones;
    }

    public Integer getAdjacentPlatinum() {
        return adjacentPlatinum;
    }

    public void setAdjacentPlatinum(Integer adjacentPlatinum) {
        this.adjacentPlatinum = adjacentPlatinum;
    }

    public Integer getMoveAblePods() {
        return moveAblePods;
    }

    public void setMoveAblePods(Integer moveAblePods) {
        this.moveAblePods = moveAblePods;
    }

    public Integer getMovedTo() {
        return movedTo;
    }

    public void setMovedTo(Integer movedTo) {
        this.movedTo = movedTo;
    }

    public Integer getDefend() {
        return defend;
    }

    public void setDefend(Integer defend) {
        this.defend = defend;
    }

    public Integer getOrderPath() {
        return orderPath;
    }

    public void setOrderPath(Integer orderPath) {
        this.orderPath = orderPath;
    }

    public Boolean getSecured() {
        return secured;
    }

    public void setSecured(Boolean secured) {
        this.secured = secured;
    }

    public Integer getValue(int myId) {
        Integer value = 0;

        if (owner == myId) { // me
            if (!secured) {
                value += platinum * 10;
                if (defend > 0) {
                    value += 10000;
                }

            }
        } else if (owner == -1) { // neutral
            value += platinum * 100;
            value += 1000 * (movedTo > 0 ? (platinum > 0 ? 1 : 0) : 1);
            value += ownedAdjacentZones;
        } else { // enemy
            if (isEmpty()) {
                value += ownedAdjacentZones;
                value += platinum * 100 * (surroundingEnnemies > 0 ? 1 : 1); // TODO TOUCHY
                value += 1000 * (movedTo > 0 ? (platinum > 0 ? 1 : 0) : 1) * (surroundingEnnemies > 0 ? 1 : 1); //TODO TOUCHY
            } else {
                value += platinum * 10;
            }
        }

        if (movedTo > 4) value = 0;

        return value;
    }

    Zone(int id, int platinum) {
        this.id = id;
        this.platinum = platinum;
    }

    public Integer[] getPods() {
        return pods;
    }


    public Integer getPlatinum() {
        return platinum;
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public List<Zone> getAdjacentZones() {
        return adjacentZones;
    }

    public boolean isEmpty() {
        return (pods[0] + pods[1] + pods[2] + pods[3] == 0);
    }

    public Integer podsOnZone() {
        return pods[0] + pods[1] + pods[2] + pods[3];
    }


    public static List<Zone> getNeutralZones(List<Zone> zones, List<Zone> platZone) {
        List<Zone> neutralAdjacentZones = new ArrayList<Zone>();
        for (Zone zone : zones) {
            if (platZone == null) {
                if (zone.getOwner() == -1) neutralAdjacentZones.add(zone);
            } else {
                if (zone.getOwner() == -1 && !platZone.contains(zone)) neutralAdjacentZones.add(zone);
            }
        }
        return neutralAdjacentZones;
    }

/*    public static List<Zone> getNotOwnedZones(List<Zone> zones, int myId) {
        List<Zone> notOwnedZones = new ArrayList<Zone>();
        for (Zone zone : zones) {
            if (zone.getOwner() != myId) notOwnedZones.add(zone);
        }
        return notOwnedZones;
    }*/

    public static List<Zone> getOwnedZones(List<Zone> zones, int myId, Boolean secured) {
        List<Zone> ownedZones = new ArrayList<Zone>();
        if (secured == null) {
            for (Zone zone : zones) {
                if (zone.getOwner() == myId) ownedZones.add(zone);
            }
        } else {
            for (Zone zone : zones) {
                if (zone.getOwner() == myId && zone.getSecured() == secured) ownedZones.add(zone);
            }
        }
        return ownedZones;
    }


    @Override
    public String toString() {
        return "/Z:" + id + " MoveAble:" + moveAblePods + " Defend:" + defend + " Pods:" + podsOnZone() + " Value:" + getValue(myId).toString();
    }

    public static Comparator<Zone> valueComparator = new Comparator<Zone>() {
        @Override
        public int compare(Zone zone1, Zone zone2) {
            return (zone1.getValue(myId) - zone2.getValue(myId)) * -1;
        }
    };

    public static Comparator<Zone> rawPlatinumComparator = new Comparator<Zone>() {
        @Override
        public int compare(Zone zone1, Zone zone2) {
            return (zone1.getPlatinum() - zone2.getPlatinum()) * -1;
        }
    };


    public static Comparator<Zone> platinumComparator = new Comparator<Zone>() {
        @Override
        public int compare(Zone zone1, Zone zone2) {  // sort lowest to highest if sortOrderForPlat >0
            return (-zone1.getPlatinum() + zone1.getAdjacentPlatinum() - zone2.getAdjacentPlatinum() + zone2.getPlatinum()) * -1;

        }
    };
}
