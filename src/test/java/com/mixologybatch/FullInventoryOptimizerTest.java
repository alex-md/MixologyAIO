package com.mixologybatch;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FullInventoryOptimizerTest
{
    static List<InventorySlot> empty() { return new ArrayList<>(Collections.nCopies(28, InventorySlot.empty())); }
    static FullInventoryOptimizer.Input input(int level,int capacity,int[] paste,long[] deficit,OptimizationGoal goal,List<InventorySlot> inventory) {
        return new FullInventoryOptimizer.Input(level,capacity,paste,deficit,goal,StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE,18,inventory);
    }
    @Test public void allUnlockThresholdsAndBothCapacities() {
        FullInventoryOptimizer engine=new FullInventoryOptimizer();
        for(int level:new int[]{59,60,62,63,65,66,68,69,71,72,74,75,77,78,80,81,99}) {
            for(int capacity:new int[]{27,28}) {
                FullInventoryOptimizer.Result result=engine.optimize(input(level,capacity,new int[]{3000,3000,3000},new long[]{13800,11200,15100},OptimizationGoal.BALANCED,empty()));
                assertEquals(level<60?0:capacity,result.plan.size());
                assertTrue(result.evaluations<=FullInventoryOptimizer.SEARCH_BUDGET);
                for(BatchEntry entry:result.plan.getEntries()) { assertTrue(entry.getPotion().level()<=level); }
                int min=capacity/3,max=(capacity+2)/3;
                if(level>=60) { for(Station station:Station.values()) {
                    long count=result.plan.getEntries().stream().filter(e->e.getStation()==station).count(); assertTrue(count>=min&&count<=max);
                } }
            }
        }
    }
    @Test public void rewardArithmeticDeficitsAndPersistence() {
        EnumMap<RewardItem,Integer> targets=new EnumMap<>(RewardItem.class);
        targets.put(RewardItem.ALDARIUM,100000); targets.put(RewardItem.REAGENT_POUCH,2);
        assertArrayEquals(new long[]{8027600,6022400,9030200},RewardTargets.costs(targets));
        assertArrayEquals(new long[]{0,20,0},RewardTargets.deficits(new long[]{10,30,40},new int[]{20,10,50}));
        assertEquals(targets,RewardTargets.parse(RewardTargets.encode(targets)));
        assertTrue(RewardTargets.parse("bad,NONE:5,ALDARIUM:-1").isEmpty());
        targets.clear(); for(RewardItem r:RewardItem.values()) { if(r!=RewardItem.NONE&&!r.isRepeatable()) { targets.put(r,1); } }
        assertArrayEquals(new long[]{61050,52550,70500},RewardTargets.costs(targets));
    }
    @Test public void objectivesChangeDistributionAndRespectResources() {
        FullInventoryOptimizer engine=new FullInventoryOptimizer();
        FullInventoryOptimizer.Result xp=engine.optimize(input(81,28,new int[]{3000,3000,3000},new long[3],OptimizationGoal.XP,empty()));
        FullInventoryOptimizer.Result reward=engine.optimize(input(81,28,new int[]{3000,3000,3000},new long[]{10000,0,0},OptimizationGoal.REWARDS,empty()));
        assertNotEquals(signature(xp.plan),signature(reward.plan));
        assertTrue(reward.resin[0]>xp.resin[0]);
        FullInventoryOptimizer.Result limited=engine.optimize(input(81,28,new int[]{300,300,300},new long[]{10,10,10},OptimizationGoal.EFFICIENCY,empty()));
        assertEquals(28,limited.plan.size()); for(int cost:limited.paste) { assertTrue(cost<=300); }
        FullInventoryOptimizer.Result partial=engine.optimize(input(60,28,new int[]{30,60,0},new long[3],OptimizationGoal.XP,empty()));
        assertEquals(3,partial.plan.size());
        FullInventoryOptimizer.Result changed=engine.optimize(input(60,27,new int[]{0,840,0},new long[3],OptimizationGoal.XP,empty()));
        assertEquals(27,changed.plan.getConfiguredCount(Potion.AAA));
    }
    @Test public void retainedInventoryAndNonPotionSlotsStayFeasible() {
        List<InventorySlot> inventory=empty();
        inventory.set(0,InventorySlot.fromItemId(Potion.MAL.getFinishedItemId()));
        inventory.set(1,InventorySlot.fromItemId(Potion.MAL.getFinishedItemId()));
        inventory.set(2,InventorySlot.fromItemId(Potion.MAL.getFinishedItemId()));
        inventory.set(3,InventorySlot.fromItemId(Potion.MAL.getFinishedItemId()));
        inventory.set(5,InventorySlot.fromItemId(995));
        FullInventoryOptimizer.Result result=new FullInventoryOptimizer().optimize(input(60,27,new int[]{3000,3000,3000},new long[3],OptimizationGoal.XP,inventory));
        assertEquals(27,result.plan.size()); assertTrue(result.plan.getConfiguredCount(Potion.MAL)>=4);
        CyclePlan cycle=CyclePlan.create(result.plan,inventory); assertTrue(cycle.isValid());
        int made=0;
        while(cycle.firstEmptySlot(inventory)>=0) {
            int slot=cycle.firstEmptySlot(inventory);
            Potion next=BatchStateResolver.nextNeededPotion(result.plan,cycle,inventory,slot); assertNotNull(next);
            inventory.set(slot,InventorySlot.fromItemId(next.getUnfinishedItemId())); made++; assertTrue(made<=23);
        }
        assertEquals(23,made); assertEquals(995,inventory.get(5).getItemId());
    }
    @Test public void searchIsReproducibleAndConveyorBonusesAreModeled() {
        FullInventoryOptimizer engine=new FullInventoryOptimizer();
        FullInventoryOptimizer.Input input=input(81,28,new int[]{3000,3000,3000},new long[]{1000,1000,1000},OptimizationGoal.REWARDS,empty());
        FullInventoryOptimizer.Result first=engine.optimize(input),second=engine.optimize(input);
        assertEquals(signature(first.plan),signature(second.plan)); assertEquals(first.xp,second.xp,0);
        assertTrue(first.deliveryClicks>0&&first.deliveryClicks<=28);
        assertTrue(first.xp>0); assertTrue(first.xp<=28*365); // unmatched deliveries get only a third XP.
    }
    @Test public void conveyorAutomaticallyUsesBonusesAndFallbackRewards() {
        FullInventoryOptimizer engine=new FullInventoryOptimizer();
        FullInventoryOptimizer.Input input=input(81,3,new int[]{3000,3000,3000},new long[3],OptimizationGoal.XP,empty());
        int[] counts=new int[10]; counts[Potion.MAL.ordinal()]=3;
        int[][][] orders=new int[1][28][3];
        for(int[] turn:orders[0]) { turn[0]=27;turn[1]=28;turn[2]=29; }
        FullInventoryOptimizer.Result matches=engine.evaluate(input,counts,orders,0);
        assertEquals(1,matches.deliveryClicks,0);assertEquals(1095,matches.xp,0);
        assertArrayEquals(new double[]{84,84,84},matches.resin,1e-9);
        for(int[] turn:orders[0]) { Arrays.fill(turn,0); }
        FullInventoryOptimizer.Result unmatched=engine.evaluate(input,counts,orders,0);
        assertEquals(3,unmatched.deliveryClicks,0);assertEquals(365,unmatched.xp,1e-9);
        assertArrayEquals(new double[]{0,0,30},unmatched.resin,1e-9);
    }
    static String signature(BatchPlan plan) { StringBuilder s=new StringBuilder(); for(Potion p:Potion.values()) { s.append(p).append(plan.getConfiguredCount(p)); } return s.toString(); }
}
