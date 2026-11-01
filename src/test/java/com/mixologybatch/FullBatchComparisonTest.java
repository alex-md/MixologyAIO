package com.mixologybatch;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independent 2,048-trial holdout evaluation, never used to select a plan. */
public class FullBatchComparisonTest
{
    @Test public void compareWithOriginalBatchAndAllMixalots() throws Exception {
        StringBuilder csv=new StringBuilder("goal,plan,distribution,xp_per_hour,mox_per_hour,aga_per_hour,lye_per_hour,clicks_per_inventory\n");
        FullInventoryOptimizer engine=new FullInventoryOptimizer();
        for(OptimizationGoal goal:new OptimizationGoal[]{OptimizationGoal.XP,OptimizationGoal.REWARDS,OptimizationGoal.BALANCED,OptimizationGoal.EFFICIENCY}) {
            FullInventoryOptimizer.Input input=FullInventoryOptimizerTest.input(81,28,new int[]{3000,3000,3000},new long[]{13800,11200,15100},goal,FullInventoryOptimizerTest.empty());
            FullInventoryOptimizer.Result optimized=engine.optimize(input);
            int[] selected=new int[10],original=new int[10],allMal=new int[10];allMal[Potion.MAL.ordinal()]=28;
            for(Potion p:Potion.values()){ selected[p.ordinal()]=optimized.plan.getConfiguredCount(p);original[p.ordinal()]=BatchPlan.defaultPlan().getConfiguredCount(p); }
            int[][] candidates={selected,original,allMal};String[] names={"search","original_counts","all_MAL"};
            for(int i=0;i<candidates.length;i++) {
                FullInventoryOptimizer.Result result=engine.evaluate(input,candidates[i],2048);
                csv.append(String.format(Locale.ROOT,"%s,%s,%s,%.1f,%.1f,%.1f,%.1f,%.2f%n",goal.name(),names[i],
                    FullInventoryOptimizerTest.signature(result.plan),result.xp*3600/result.seconds,result.resin[0]*3600/result.seconds,
                    result.resin[1]*3600/result.seconds,result.resin[2]*3600/result.seconds,result.deliveryClicks));
                assertEquals(28,result.plan.size());assertTrue(Double.isFinite(result.score));
            }
        }
        Path report=Paths.get("build/reports/full-batch-comparison.csv");Files.createDirectories(report.getParent());Files.write(report,csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
