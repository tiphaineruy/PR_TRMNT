import java.util.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 */
class Player {

    public static int myId = -1;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int playerCount = in.nextInt(); // the amount of players (2 to 4)
        myId = in.nextInt(); // my player ID (0, 1, 2 or 3)
        int zoneCount = in.nextInt(); // the amount of zones on the map
        int linkCount = in.nextInt(); // the amount of links between all zones

        List<Zone> zones = new ArrayList<Zone>();
        List<Zone> platinumZones = new ArrayList<Zone>();
        List<Zone> currentOwnedPodsZones = new ArrayList<Zone>();
        List<Zone> noPodsZones = new ArrayList<Zone>();


        in.nextLine();
        for (int i = 0; i < zoneCount; i++) {
            int zoneId = in.nextInt(); // this zone's ID (between 0 and zoneCount-1)
            int platinumSource = in.nextInt(); // the amount of Platinum this zone can provide per game turn
            zones.add(new Zone(zoneId, platinumSource));
            if (platinumSource > 0) platinumZones.add(zones.get(zoneId)); // added to the list of profitable zones
            in.nextLine();
        }

        for (int i = 0; i < linkCount; i++) {
            Zone zone1 = zones.get(in.nextInt());
            Zone zone2 = zones.get(in.nextInt());
            zone1.getAdjacentZones().add(zone2);
            zone2.getAdjacentZones().add(zone1);
            in.nextLine();
        }

        Collections.sort(platinumZones, new Comparator<Zone>() {
            @Override
            public int compare(Zone zone1, Zone zone2) {
                return zone1.getPlatinum().compareTo(zone2.getPlatinum());
            }
        });

        System.err.println("PLAT ZONES:" + platinumZones);
        System.err.println("ZONES:" + zones);

        // game loop
        while (true) {
            noPodsZones = new ArrayList<Zone>();
            currentOwnedPodsZones = new ArrayList<Zone>();
            int platinum = in.nextInt(); // my available Platinum
            int maxPods = (new Double(platinum / 20)).intValue();

            System.err.println("PLAT:" + platinum + " MAX_PODS:" + maxPods);

            // UPDATE /////////////////////////////////////////
            in.nextLine();
            for (int i = 0; i < zoneCount; i++) {
                int zId = in.nextInt(); // this zone's ID
                int ownerId = in.nextInt(); // the player who owns this zone (-1 otherwise)
                int podsP0 = in.nextInt(); // player 0's PODs on this zone
                int podsP1 = in.nextInt(); // player 1's PODs on this zone
                int podsP2 = in.nextInt(); // player 2's PODs on this zone (always 0 for a two player game)
                int podsP3 = in.nextInt(); // player 3's PODs on this zone (always 0 for a two or three player game)
                // updating zones
                Zone zone = zones.get(zId);
                zone.setOwner(ownerId);
                zone.getPods()[0] = podsP0;
                zone.getPods()[1] = podsP1;
                zone.getPods()[2] = podsP2;
                zone.getPods()[3] = podsP3;
                if (zone.getPods()[myId] > 0) currentOwnedPodsZones.add(zone);
                if (zone.isEmpty()) noPodsZones.add(zone);
                // reset ordering path
                zone.setOrderPath(-1);
                in.nextLine();
            }


            // SECURED /////////////////////////////////////
            List<Zone> unsecured = new ArrayList<Zone>();
            List<Zone> secured = new ArrayList<Zone>();
            for (Zone zone : Zone.getOwnedZones(zones, myId, null)) {
                zone.setSecured(true);
                for (Zone adj : zone.getAdjacentZones()) {
                    if (adj.getOwner() != myId) {
                        zone.setSecured(false);
                        unsecured.add(zone);
                        zone.setOrderPath(0);
                        break;
                    }
                }
                if (zone.getSecured()) secured.add(zone);
            }

            System.err.println("UNSECURED:"+unsecured);
            System.err.println("SECURED:"+secured);
            // DENSITY /////////////////////
            for (int i = 0; i < zoneCount; i++) {
                Zone zone = zones.get(i);
                if (zone.getOwner() == myId) {
                    int density = 0;
                    density += zone.getSecured()? 1000 : 0;
                    density -= zone.getPlatinum()*100;
//                    density += zone.getPods()[myId]*10;
//                    density += zone.getSecured()?zone.getEdges()*1000:0;
                    zone.setMyDensity(density);
                } else {
                    zone.setMyDensity(0);
                }
            }

            List<Zone> temp = new ArrayList<Zone>();
            temp.addAll(secured);
            //SET_ORDERING_PATH
            int lvl = 0;
            while(!temp.isEmpty()&& lvl<100){
                lvl+=1;
                Set<Zone> toRemove = new HashSet<Zone>();
                for (Zone zone :  temp){
                    for( Zone adjacent: zone.getAdjacentZones()){
                        if (adjacent.getOrderPath() == lvl-1){
                            toRemove.add(zone);
                            zone.setOrderPath(lvl);
                            break;
                        }
                    }
                }
                temp.removeAll(toRemove);
            }
            for (Zone zone:secured){
                System.err.println("SCEURED ZONE:"+zone+" "+zone.getOrderPath());
            }

            // MOVE /////////////////////////////////////////////
            StringBuilder moveOut = new StringBuilder();
            Map<Movement, Integer> moves = new HashMap<Movement, Integer>();
            for (Zone zone : currentOwnedPodsZones) { // for each zone where I have pods
                List<Zone> priorities;
                if (zone.getSecured()) {
                    priorities = buildOrderPathMoveListForZone(zone.getAdjacentZones());
                } else {
                    priorities = buildPriorityMoveListForZone(zone);
                }
                int options = priorities.size();
                int currentOption = 0;

                //building pods move command*
                int movingPods = zone.getPods()[myId];
                if (zone.getSecured()){
                    for (int i = 0; i < movingPods; i++) {
                        Movement.addMove(zone.getId(), priorities.get(0).getId(), moves);
                    }
                }else{
                    movingPods -= zone.getPlatinum();
//                    if (movingPods>options) movingPods -=1;
                    for (int i = 0; i < movingPods; i++) { // for each pod of the zone
                        if(options>0){
                            if(currentOption == options) currentOption = 0;
                            if (currentOption < options) {
                                Movement.addMove(zone.getId(), priorities.get(currentOption).getId(), moves);
                                currentOption+=1;
                            }
                        }
                    }
                }
            }
            if (moves.isEmpty()) {
                System.out.println("WAIT");
            } else {
                for(Movement movement:moves.keySet()){
                    moveOut.append(moves.get(movement)+" " + movement.getFrom() + " " + movement.getTo()+ " ");
                }
                System.out.println(moveOut.toString().trim());
            }


            int DROP_SIZE = 1;
            // BUY ////////////////////////////////////////////////
            StringBuilder buyOut = new StringBuilder();
            List<Zone> dropTo = new ArrayList<Zone>();
            dropTo.addAll(Zone.getNeutralZones(platinumZones, null));
            List<Zone> mines = Zone.getOwnedZones(zones, myId, false);
            dropTo.addAll(buildDensityMoveListForZone(mines));
            if (!dropTo.isEmpty()) {
                //buy
                Integer currentDropSize = 0;
                Integer currentPlatZone = 0;
                for (int i = 0; i < maxPods; i++) {
                    currentDropSize += 1;
                    if (currentDropSize == DROP_SIZE) {
                        buyOut.append(currentDropSize + " " + dropTo.get(currentPlatZone).getId() + " ");
                        currentDropSize = 0;
                        if (currentPlatZone < dropTo.size() - 1) {
                            currentPlatZone += 1;
                        }

                    }
                }
                if (currentDropSize < DROP_SIZE && currentDropSize > 0)
                    buyOut.append(currentDropSize + " " + dropTo.get(currentPlatZone).getId() + " ");
            }

            System.out.println(buyOut.toString().trim());
        }
    }

    public static List<Zone> buildPriorityMoveListForZone(final Zone zone) {
        List<Zone> priorityMoves = new ArrayList<Zone>();
        priorityMoves.addAll(Zone.getNotOwnedZones(zone.getAdjacentZones(),myId));
        Collections.sort(priorityMoves, new Comparator<Zone>() {
            @Override
            public int compare(Zone zone1, Zone zone2) {
                return (zone1.getValue(myId) - zone2.getValue(myId)) * -1;
            }
        });
        return priorityMoves;
    }

    public static List<Zone> buildDensityMoveListForZone(final List<Zone> zones) {
        List<Zone> priorityMoves = new ArrayList<Zone>();
        priorityMoves.addAll(zones);
        Collections.sort(priorityMoves, new Comparator<Zone>() {
            @Override
            public int compare(Zone zone1, Zone zone2) {
                return (zone1.getMyDensity() - zone2.getMyDensity());
            }
        });
        return priorityMoves;
    }

    public static List<Zone> buildOrderPathMoveListForZone(final List<Zone> zones) {
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

    public static void addMove(int from, int to, Map<Movement, Integer> moves) {
        addMouv(new Integer(from), new Integer(to), moves);
    }

    public static void addMouv(Integer from, Integer to, Map<Movement, Integer> moves) {
        if (moves.get(new Movement(from, to)) != null) {
            moves.put(new Movement(from, to), moves.get(new Movement(from, to)) + 1);
        } else {
            moves.put(new Movement(from, to), 1);
        }
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
    private List<Zone> adjacentZones = new ArrayList<Zone>();
    private int id;
    private Integer platinum = 0;
    private int owner = -1;
    private Integer[] pods = new Integer[]{0, 0, 0, 0};
    private Integer myDensity = 0;
    private Boolean secured = false;
    private Integer orderPath = -1;

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

    public Integer getMyDensity() {
        return myDensity;
    }

    public void setMyDensity(Integer myDensity) {
        this.myDensity = myDensity;
    }

    public int getEdges() {
        return 6 - adjacentZones.size();
    }

    public Integer getValue(int myId) {
        Integer value = 0;

        if (owner == myId) { // me
            if (!secured)
                value += platinum * 10;
        } else if (owner == -1) { // neutral
            value += platinum * 100;
            value += 1000;
        } else { // enemy
            if (isEmpty()) {
                value += platinum * 100;
                value += 1001;
            } else {

            }
        }

        return value;
    }

    Zone(int id, int platinum) {
        this.id = id;
        this.platinum = platinum;
    }

    public Integer[] getPods() {
        return pods;
    }

    public void setPods(Integer[] pods) {
        this.pods = pods;
    }

    public Integer getPlatinum() {
        return platinum;
    }

    public void setPlatinum(Integer platinum) {
        this.platinum = platinum;
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

    public void setAdjacentZones(List<Zone> adjacentZones) {
        this.adjacentZones = adjacentZones;
    }

    public boolean isEmpty() {
        return (pods[0] + pods[1] + pods[2] + pods[3] == 0);
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

    public static List<Zone> getNotOwnedZones(List<Zone> zones, int myId) {
        List<Zone> notOwnedZones = new ArrayList<Zone>();
        for (Zone zone : zones) {
            if (zone.getOwner() != myId) notOwnedZones.add(zone);
        }
        return notOwnedZones;
    }

    public static List<Zone> getOrderedPlatinumZones(List<Zone> zones) {
        List<Zone> platinumZones = new ArrayList<Zone>();
        for (Zone zone : zones) {
            if (zone.getPlatinum() >= 1) platinumZones.add(zone);
        }
        Collections.sort(platinumZones, new Comparator<Zone>() {
            @Override
            public int compare(Zone zone1, Zone zone2) {
                return zone1.getPlatinum().compareTo(zone2.getPlatinum());
            }
        });
        return platinumZones;
    }

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

    public static List<Zone> getEmptyFromPods(List<Zone> zones, List<Zone> noPodsZones) {
        List<Zone> noPodZonesInList = new ArrayList<Zone>();
        for (Zone zone : zones) {
            if (noPodsZones.contains(zone)) noPodZonesInList.add(zone);
        }
        return noPodZonesInList;
    }

    @Override
    public String toString() {
        return id + "";
    }
}
