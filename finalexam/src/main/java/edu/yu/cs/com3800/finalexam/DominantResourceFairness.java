package edu.yu.cs.com3800.finalexam;

import java.util.*;

/**
 * Implements a single run of the DRF algorithm.
 */
public class DominantResourceFairness {

    /**
     * Describes the allocation of units of a single resource to a user
     */
    public class Allocation{
        String resourceName;
        double unitsAllocated;
        User user;
        public Allocation(String resourceName, double unitsAllocated, User user){
            this.resourceName = resourceName;
            this.unitsAllocated = unitsAllocated;
            this.user = user;
        }
        public String toString(){
            return this.unitsAllocated + " of " + this.resourceName + " allocated to " + this.user;
        }
    }

    /**a map of the resources that exist in this system. Key is the resource's name, value is the actial resource object*/
    private Map<String, SystemResource> systemResources;
    /**Users in the system, sorted by their dominant share. Note: to re-sort the users in the TreeSet,
     * when a User's dominant share changes, you must remove it from the TreeSet and re-add it*/
    private TreeSet<User> users;

    /**
     * @param systemResources
     * @param users
     * @throws IllegalArgumentException if either collection is empty or null
     */
    public DominantResourceFairness(Map<String, SystemResource> systemResources, TreeSet<User> users){
        if ( systemResources==null || systemResources.isEmpty() ){
            throw new IllegalArgumentException("system resources is empty");
        }
        if ( users==null || users.isEmpty() ){
            throw new IllegalArgumentException("no users");
        }
        this.systemResources=systemResources;
        this.users =users;
    }

    /**
     * Repeatedly allocate resources to the user with the lowest dominant share, until there are
     * insufficient unallocated resources remaining to meet any user's requirements.
     * @return a list of the individual resource allocations made by DRF, in order
     */
    public List<Allocation> allocateResources(){
        ArrayList<Allocation>  allocations = new ArrayList<>();
        User user = users.pollFirst();


        while (ifAvailable(user)){
            ArrayList<Resource> share = new ArrayList<>();
            users.remove(user);
            for (Map.Entry<String, Resource> resources: user.getRequiredResourcesPerTask().entrySet()) {
                    Resource resource = resources.getValue();
                    share.add(resource);
                    allocations.add(new Allocation(resource.getName(), resource.getUnits(), user));
                    SystemResource sr = systemResources.get(resource.getName());
                    sr.allocate(resource.getUnits());
                    systemResources.put(resource.getName(), sr);
            }
            user.allocateResources(share);
            users.add(user);
            user= users.pollFirst();
        }
        return allocations;
    }



    private boolean ifAvailable(User user) {
        for (Map.Entry<String, Resource> resources: user.getRequiredResourcesPerTask().entrySet()) {

            if (systemResources.get(resources.getKey()).getAvailable()<resources.getValue().getUnits()){
                return false;
            }
        }
        return true;
    }
}
