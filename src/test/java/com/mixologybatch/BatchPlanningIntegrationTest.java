package com.mixologybatch;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class BatchPlanningIntegrationTest
{
    private static class Settings implements MixologyBatchConfig {
        OptimizationGoal goal=OptimizationGoal.XP;
        String rewards="";
        public OptimizationGoal goal(){return goal;}
        public String rewardTargets(){return rewards;}
    }
    @Test public void queueStaysStableUntilRollingRefillAndManualOverrideIsDeferred() throws Exception {
        Settings settings=new Settings(); Map<Integer,Integer> bits=new HashMap<>();
        bits.put(VarbitID.MM_AVAILABLE_MOX,3000); bits.put(VarbitID.MM_AVAILABLE_AGA,3000); bits.put(VarbitID.MM_AVAILABLE_LYE,3000);
        int[] level={81}; Item[][] items={new Item[28]};
        Player player=proxy(Player.class,(p,m,a)->m.getName().equals("getWorldLocation")?new WorldPoint(1394,9326,0):null);
        Widget widget=proxy(Widget.class,(p,m,a)->m.getName().equals("isSelfHidden")?false:null);
        ItemContainer container=proxy(ItemContainer.class,(p,m,a)->items[0]);
        Client client=proxy(Client.class,(p,m,a)->{
            switch(m.getName()) {
                case "getLocalPlayer":return player;
                case "getWidget":return widget;
                case "getItemContainer":return container;
                case "getBoostedSkillLevel":return level[0];
                case "getVarbitValue":return bits.getOrDefault((Integer)a[0],0);
                case "getVarpValue":return 0;
                default:throw new UnsupportedOperationException(m.getName());
            }
        });
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            MixologyBatchPlugin plugin=new MixologyBatchPlugin(); inject(plugin,"client",client);inject(plugin,"config",settings);inject(plugin,"plannerWorker",worker);
            update(plugin); awaitPlanner(plugin); update(plugin);
            BatchPlan original=plugin.getPlan(); assertEquals(28,original.size());
            assertEquals(Guidance.Phase.MIXING,plugin.getGuidance().getPhase());
            Potion first=plugin.getGuidance().getEntry().getPotion(); items[0][0]=new Item(first.getUnfinishedItemId(),1);
            update(plugin);
            settings.goal=OptimizationGoal.MANUAL; settings.rewards="REAGENT_POUCH:2,"; level[0]=99;
            bits.put(VarbitID.MM_AVAILABLE_MOX,2500); bits.put(VarbitID.MM_LAB_ORDER_1_TYPE,1);
            update(plugin); assertSame(original,plugin.getPlan());
            // A finished full inventory proceeds through the original conveyor workflow.
            for(int i=0;i<28;i++){items[0][i]=new Item(original.get(i).getPotion().getFinishedItemId(),1);}
            update(plugin); assertEquals(Guidance.Action.DEPOSIT,plugin.getGuidance().getAction());
            assertSame(original,plugin.getPlan());
            items[0]=new Item[28]; update(plugin);
            assertNotSame(original,plugin.getPlan()); assertEquals(28,plugin.getPlan().size());
            assertEquals(6,plugin.getPlan().getConfiguredCount(Potion.MAL));
            assertEquals(4,plugin.getPlan().getConfiguredCount(Potion.MML));
        } finally {worker.shutdownNow();}
    }
    @Test public void noPastePlanResumesAfterLoadingHopper() throws Exception {
        // Direct boundary adapter test also verifies worker results are adopted only after completion.
        MixologyBatchPlugin plugin=new MixologyBatchPlugin(); Settings settings=new Settings();
        inject(plugin,"config",settings); inject(plugin,"observedLevel",81);
        ExecutorService worker=Executors.newSingleThreadExecutor(); inject(plugin,"plannerWorker",worker);
        Method prepare=MixologyBatchPlugin.class.getDeclaredMethod("prepareRefill",List.class);prepare.setAccessible(true);
        try {
            assertEquals(false,prepare.invoke(plugin,FullInventoryOptimizerTest.empty())); awaitPlanner(plugin);
            assertEquals(true,prepare.invoke(plugin,FullInventoryOptimizerTest.empty())); assertEquals(0,plugin.getPlan().size());
            inject(plugin,"hopperPaste",new int[]{3000,3000,3000});
            assertEquals(false,prepare.invoke(plugin,FullInventoryOptimizerTest.empty())); awaitPlanner(plugin);
            assertEquals(true,prepare.invoke(plugin,FullInventoryOptimizerTest.empty())); assertEquals(28,plugin.getPlan().size());
        } finally {worker.shutdownNow();}
    }
    private static void awaitPlanner(Object plugin)throws Exception {
        Field field=plugin.getClass().getDeclaredField("pendingPlan");field.setAccessible(true);((Future<?>)field.get(plugin)).get(5,TimeUnit.SECONDS);
    }
    private static void update(Object plugin)throws Exception {
        Method method=plugin.getClass().getDeclaredMethod("updateState");method.setAccessible(true);method.invoke(plugin);
    }
    private static void inject(Object target,String name,Object value)throws Exception {Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    @SuppressWarnings("unchecked") private static <T>T proxy(Class<T>type,InvocationHandler handler){return(T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
}
