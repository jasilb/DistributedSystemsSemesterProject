import edu.yu.cs.com3800.finalexam.DominantResourceFairness;
import edu.yu.cs.com3800.finalexam.Resource;
import edu.yu.cs.com3800.finalexam.SystemResource;
import edu.yu.cs.com3800.finalexam.User;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class DRFTest {
    @Test
    public void test1() {
        String CPU = "cpu";
        String RAM = "ram";

        //create system resources
        HashMap<String, SystemResource> systemResources = new HashMap<>(2);
        SystemResource systemCPU = new SystemResource(CPU,12);
        systemResources.put(CPU,systemCPU);
        SystemResource systemRAM = new SystemResource(RAM,12);
        systemResources.put(RAM,systemRAM);

        TreeSet<User> users = new TreeSet<User>();
        //create user 1
        HashMap<String, Resource> userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,1));
        userResources.put(RAM,new Resource(RAM,3));
        users.add(new User("User A", userResources,systemResources));
        //create user 2
        userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,2));
        userResources.put(RAM,new Resource(RAM,2));
        users.add(new User("User B",userResources,systemResources));
        //create user 3
         userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,3));
        userResources.put(RAM,new Resource(RAM,1));
        users.add(new User("User C", userResources,systemResources));
        //run DRF
        DominantResourceFairness drf = new DominantResourceFairness(systemResources,users);
        List<DominantResourceFairness.Allocation> allocations = drf.allocateResources();

        //check results
        String results = "";
        for(DominantResourceFairness.Allocation allocation : allocations){
            System.out.println(allocation);
            results += allocation + "\n";
        }

        //String expected = "1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n3.0 of cpu allocated to User B\n1.0 of ram allocated to User B\n1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n3.0 of cpu allocated to User B\n1.0 of ram allocated to User B\n1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n";
        //assert results.equals(expected);
        double allocatedCPU = systemCPU.getUnits() - systemCPU.getAvailable();
        double allocatedRAM = systemRAM.getUnits() - systemRAM.getAvailable();
        System.out.println("allocated CPU: "+ allocatedCPU);
        System.out.println("allocated RAM: "+ allocatedRAM);
        //assert allocatedRAM == 14;
        //assert allocatedCPU == 9;
    }
    @Test
    public void test2() {
        String CPU = "cpu";
        String RAM = "ram";

        //create system resources
        HashMap<String, SystemResource> systemResources = new HashMap<>(2);
        SystemResource systemCPU = new SystemResource(CPU,12);
        systemResources.put(CPU,systemCPU);
        SystemResource systemRAM = new SystemResource(RAM,12);
        systemResources.put(RAM,systemRAM);

        TreeSet<User> users = new TreeSet<User>();
        //create user 1
        HashMap<String, Resource> userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,1));
        userResources.put(RAM,new Resource(RAM,3));
        users.add(new User("User A", userResources,systemResources));
        //create user 2
        userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,2));
        userResources.put(RAM,new Resource(RAM,2));
        users.add(new User("User B",userResources,systemResources));
        //create user 3
        userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,3));
        userResources.put(RAM,new Resource(RAM,1));
        users.add(new User("User C", userResources,systemResources));
        //run DRF
        DominantResourceFairness drf = new DominantResourceFairness(systemResources,users);
        List<DominantResourceFairness.Allocation> allocations = drf.allocateResources();

        //check results
        String results = "";
        for(DominantResourceFairness.Allocation allocation : allocations){
            System.out.println(allocation);
            results += allocation + "\n";
        }

        //String expected = "1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n3.0 of cpu allocated to User B\n1.0 of ram allocated to User B\n1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n3.0 of cpu allocated to User B\n1.0 of ram allocated to User B\n1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n";
        //assert results.equals(expected);
        double allocatedCPU = systemCPU.getUnits() - systemCPU.getAvailable();
        double allocatedRAM = systemRAM.getUnits() - systemRAM.getAvailable();
        System.out.println("allocated CPU: "+ allocatedCPU);
        System.out.println("allocated RAM: "+ allocatedRAM);
        //assert allocatedRAM == 14;
        //assert allocatedCPU == 9;
    }
    @Test
    public void test3() {
        String CPU = "cpu";
        String RAM = "ram";
        String FAN = "fan";

        //create system resources
        HashMap<String,SystemResource> systemResources = new HashMap<>(3);
        SystemResource systemCPU = new SystemResource(CPU,10);
        systemResources.put(CPU,systemCPU);
        SystemResource systemRAM = new SystemResource(RAM,20);
        systemResources.put(RAM,systemRAM);
        SystemResource systemFAN = new SystemResource(FAN,40);
        systemResources.put(FAN,systemFAN);

        TreeSet<User> users = new TreeSet<User>();
        //create user 1
        HashMap<String,Resource> userResources = new HashMap<>(3);
        userResources.put(CPU,new Resource(CPU,1));
        userResources.put(RAM,new Resource(RAM,4));
        userResources.put(FAN,new Resource(FAN,3));
        users.add(new User("User A", userResources,systemResources));
        //create user 2
        userResources = new HashMap<>(3);
        userResources.put(CPU,new Resource(CPU,3));
        userResources.put(RAM,new Resource(RAM,1));
        userResources.put(FAN,new Resource(FAN,10));
        users.add(new User("User B",userResources,systemResources));

        //run DRF
        DominantResourceFairness drf = new DominantResourceFairness(systemResources,users);
        List<DominantResourceFairness.Allocation> allocations = drf.allocateResources();

        //check results
        String results = "";
        for(DominantResourceFairness.Allocation allocation : allocations){
            System.out.println(allocation);
            results += allocation + "\n";
        }
        double allocatedCPU = systemCPU.getUnits() - systemCPU.getAvailable();
        double allocatedRAM = systemRAM.getUnits() - systemRAM.getAvailable();
        double allocatedFAN = systemFAN.getUnits() - systemFAN.getAvailable();
        System.out.println("CPU: "+allocatedCPU );
        System.out.println("RAM: "+allocatedRAM );
        System.out.println("FAN: "+allocatedFAN );

    }
    @Test
    public void test3_1() {
        String CPU = "cpu";
        String RAM = "ram";
        String FAN = "fan";

        //create system resources
        HashMap<String,SystemResource> systemResources = new HashMap<>(3);
        SystemResource systemCPU = new SystemResource(CPU,9);
        systemResources.put(CPU,systemCPU);
        SystemResource systemRAM = new SystemResource(RAM,18);
        systemResources.put(RAM,systemRAM);
        SystemResource systemFAN = new SystemResource(FAN,27);
        systemResources.put(FAN,systemFAN);

        TreeSet<User> users = new TreeSet<User>();
        //create user 1
        HashMap<String,Resource> userResources = new HashMap<>(3);
        userResources.put(CPU,new Resource(CPU,1));
        userResources.put(RAM,new Resource(RAM,4));
        userResources.put(FAN,new Resource(FAN,3));
        users.add(new User("User A", userResources,systemResources));
        //create user 2
        userResources = new HashMap<>(3);
        userResources.put(CPU,new Resource(CPU,3));
        userResources.put(RAM,new Resource(RAM,1));
        userResources.put(FAN,new Resource(FAN,2));
        users.add(new User("User B",userResources,systemResources));
        userResources = new HashMap<>(3);
        userResources.put(CPU,new Resource(CPU,1));
        userResources.put(RAM,new Resource(RAM,1));
        userResources.put(FAN,new Resource(FAN,4));
        users.add(new User("User C",userResources,systemResources));

        //run DRF
        DominantResourceFairness drf = new DominantResourceFairness(systemResources,users);
        List<DominantResourceFairness.Allocation> allocations = drf.allocateResources();

        //check results
        String results = "";
        for(DominantResourceFairness.Allocation allocation : allocations){
            System.out.println(allocation);
            results += allocation + "\n";
        }
        double allocatedCPU = systemCPU.getUnits() - systemCPU.getAvailable();
        double allocatedRAM = systemRAM.getUnits() - systemRAM.getAvailable();
        double allocatedFAN = systemFAN.getUnits() - systemFAN.getAvailable();
        System.out.println("CPU: "+systemCPU.getAvailable() );
        System.out.println("RAM: "+systemRAM.getAvailable() );
        System.out.println("FAN: "+systemFAN.getAvailable() );

    }
    @Test (expected = IllegalArgumentException.class)
    public void testNull() {
        String CPU = "cpu";
        String RAM = "ram";

        //create system resources
        HashMap<String,SystemResource> systemResources = new HashMap<>(2);
        SystemResource systemCPU = new SystemResource(CPU,9);
        systemResources.put(CPU,systemCPU);
        SystemResource systemRAM = new SystemResource(RAM,18);
        systemResources.put(RAM,systemRAM);

        TreeSet<User> users = new TreeSet<User>();
        //create user 1
        HashMap<String,Resource> userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,1));
        userResources.put(RAM,new Resource(RAM,4));
        users.add(new User("User A", userResources,systemResources));
        //create user 2
        userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,3));
        userResources.put(RAM,new Resource(RAM,1));
        users.add(new User("User B",userResources,systemResources));

        //run DRF
        DominantResourceFairness drf = new DominantResourceFairness(null,users);
        List<DominantResourceFairness.Allocation> allocations = drf.allocateResources();
    }
    @Test
    public void testEmpty() {
        String CPU = "cpu";
        String RAM = "ram";

        //create system resources
        HashMap<String,SystemResource> systemResources = new HashMap<>(2);
        SystemResource systemCPU = new SystemResource(CPU,0);
        systemResources.put(CPU,systemCPU);
        SystemResource systemRAM = new SystemResource(RAM,122);
        systemResources.put(RAM,systemRAM);

        TreeSet<User> users = new TreeSet<User>();
        //create user 1
        HashMap<String,Resource> userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,1));
        userResources.put(RAM,new Resource(RAM,4));
        users.add(new User("User A", userResources,systemResources));
        //create user 2
        userResources = new HashMap<>(2);
        userResources.put(CPU,new Resource(CPU,3));
        userResources.put(RAM,new Resource(RAM,1));
        users.add(new User("User B",userResources,systemResources));

        //run DRF
        DominantResourceFairness drf = new DominantResourceFairness(systemResources,users);
        List<DominantResourceFairness.Allocation> allocations = drf.allocateResources();

        //check results
        String results = "";
        for(DominantResourceFairness.Allocation allocation : allocations){
            System.out.println(allocation);
            results += allocation + "\n";
        }
        assert results == "";
    }
}
