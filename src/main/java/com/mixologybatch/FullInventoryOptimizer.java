package com.mixologybatch;

import java.util.*;

/** Full-inventory search using common seeded conveyor simulations; never plans live-order actions. */
final class FullInventoryOptimizer
{
    private static final Potion[] POTIONS = Potion.values();
    static final int SEARCH_BUDGET = 384;
    static final int TRIALS = 64;
    static final class Input {
        final int level, capacity, processingSeconds;
        final int[] paste;
        final long[] deficit;
        final OptimizationGoal goal;
        final StationOrder stationOrder;
        final List<InventorySlot> inventory;
        Input(int level, int capacity, int[] paste, long[] deficit, OptimizationGoal goal,
              StationOrder stationOrder, int processingSeconds, List<InventorySlot> inventory) {
            this.level=level; this.capacity=capacity; this.paste=paste.clone(); this.deficit=deficit.clone();
            this.goal=goal; this.stationOrder=stationOrder; this.processingSeconds=processingSeconds;
            this.inventory=Collections.unmodifiableList(new ArrayList<>(inventory));
        }
    }
    static final class Result {
        final BatchPlan plan;
        final double xp, seconds, score, deliveryClicks;
        final double[] resin;
        final int[] paste;
        final int evaluations;
        Result(BatchPlan plan,double xp,double seconds,double score,double clicks,double[] resin,int[] paste,int evaluations) {
            this.plan=plan; this.xp=xp; this.seconds=seconds; this.score=score; this.deliveryClicks=clicks;
            this.resin=resin.clone(); this.paste=paste.clone(); this.evaluations=evaluations;
        }
    }
    Result optimize(Input input) {
        int[] lower=new int[Potion.values().length];
        for (InventorySlot slot:input.inventory) { if(slot.isPotion()) { lower[slot.getPotion().ordinal()]++; } }
        List<Potion> eligible=new ArrayList<>();
        for(Potion p:Potion.values()) { if(p.level()<=input.level) { eligible.add(p); } }
        if(eligible.isEmpty()) { return evaluate(input,lower,0); }
        int[][][] streams=streams(input.level,TRIALS,0x4d49584cL);
        List<int[]> seeds=new ArrayList<>();
        BatchPlan reference = BatchPlan.defaultPlan();
        // Diverse feasible starts: weighted demand, original batch, resin pressure, XP, and each recipe.
        for(int strategy=0;strategy<eligible.size()+4;strategy++) {
            if (Thread.currentThread().isInterrupted()) { throw new java.util.concurrent.CancellationException(); }
            int[] counts=lower.clone(), remaining=input.paste.clone();
            Random random=new Random(51+strategy);
            while(sum(counts)<input.capacity) {
                Potion best=null; double score=-Double.MAX_VALUE;
                for(Potion p:eligible) {
                    if(!affordable(p,remaining)) { continue; }
                    double value;
                    if(strategy==0) { value=p.weight()/(1.0+counts[p.ordinal()]); }
                    else if(strategy==1) { value=reference.getConfiguredCount(p)/(1.0+counts[p.ordinal()]); }
                    else if(strategy==2) {
                        value=.01;
                        long max=Math.max(1,Arrays.stream(input.deficit).max().orElse(0));
                        for(int c=0;c<3;c++) { value+=p.resin(c)*input.deficit[c]/(double)max; }
                        value/=1+counts[p.ordinal()]*.1;
                    }
                    else if(strategy==3) { value=p.xp()/(1.0+counts[p.ordinal()]*.1); }
                    else { value=p==eligible.get(strategy-4)?1000:random.nextDouble(); }
                    if(value>score) { score=value; best=p; }
                }
                if(best==null) { break; }
                counts[best.ordinal()]++;
                for(int c=0;c<3;c++) { remaining[c]-=best.paste(c); }
            }
            seeds.add(counts);
        }
        // Find a full feasible seed even when greedy fills strand small paste fragments.
        int[] feasible=lower.clone();
        int needed=Math.max(0,input.capacity-sum(lower));
        if(fill(needed,input.paste.clone(),eligible,feasible,new HashSet<>())) { seeds.add(feasible); }
        int bestSize=0;
        for(int[] seed:seeds) { bestSize=Math.max(bestSize,sum(seed)); }
        Result best=null; int[] bestCounts=null; int evaluations=0;
        for(int[] seed:seeds) {
            if(sum(seed)!=bestSize) { continue; }
            Result candidate=evaluate(input,seed,streams,++evaluations);
            if(best==null||candidate.score>best.score) { best=candidate; bestCounts=seed; }
        }
        // Best-improving one-potion exchanges, subject to paste and retained-inventory lower bounds.
        boolean improved=true;
        while(improved&&evaluations<SEARCH_BUDGET) {
            if (Thread.currentThread().isInterrupted()) { throw new java.util.concurrent.CancellationException(); }
            improved=false; Result next=best; int[] nextCounts=bestCounts;
            for(Potion remove:Potion.values()) {
                if(bestCounts[remove.ordinal()]<=lower[remove.ordinal()]) { continue; }
                for(Potion add:eligible) {
                    if(add==remove||evaluations>=SEARCH_BUDGET) { continue; }
                    int[] counts=bestCounts.clone(); counts[remove.ordinal()]--; counts[add.ordinal()]++;
                    if(!feasible(input,counts,lower)) { continue; }
                    Result candidate=evaluate(input,counts,streams,++evaluations);
                    if(candidate.score>next.score+1e-8) { next=candidate; nextCounts=counts; improved=true; }
                }
            }
            best=next; bestCounts=nextCounts;
        }
        return new Result(best.plan,best.xp,best.seconds,best.score,best.deliveryClicks,best.resin,best.paste,evaluations);
    }
    // At most 28 additions. Failed subproblems are memoized; pure recipes give a fast feasible fallback.
    private static boolean fill(int n,int[] paste,List<Potion> eligible,int[] counts,Set<String> failed) {
        if (Thread.currentThread().isInterrupted()) { throw new java.util.concurrent.CancellationException(); }
        if(n==0) { return true; }
        if(paste[0]+paste[1]+paste[2]<n*30) { return false; }
        if(failed.size()>20000) { return false; }
        String key=n+":"+Math.min(paste[0],n*30)+":"+Math.min(paste[1],n*30)+":"+Math.min(paste[2],n*30);
        if(failed.contains(key)) { return false; }
        for(Potion p:eligible) {
            if(!affordable(p,paste)) { continue; }
            for(int c=0;c<3;c++) { paste[c]-=p.paste(c); } counts[p.ordinal()]++;
            if(fill(n-1,paste,eligible,counts,failed)) { return true; }
            for(int c=0;c<3;c++) { paste[c]+=p.paste(c); } counts[p.ordinal()]--;
        }
        failed.add(key); return false;
    }
    private static boolean affordable(Potion p,int[] paste) { return p.paste(0)<=paste[0]&&p.paste(1)<=paste[1]&&p.paste(2)<=paste[2]; }
    private static boolean feasible(Input input,int[] counts,int[] lower) {
        int[] cost=cost(counts,lower);
        return cost[0]<=input.paste[0]&&cost[1]<=input.paste[1]&&cost[2]<=input.paste[2];
    }
    private static int[] cost(int[] counts,int[] lower) {
        int[] result=new int[3];
        for(Potion p:Potion.values()) { for(int c=0;c<3;c++) { result[c]+=Math.max(0,counts[p.ordinal()]-lower[p.ordinal()])*p.paste(c); } }
        return result;
    }
    private static int sum(int[] counts) { return Arrays.stream(counts).sum(); }
    static int[][][] streams(int level,int trials,long seed) {
        List<Potion> weighted=new ArrayList<>();
        for(Potion p:Potion.values()) { if(p.level()<=level) { for(int i=0;i<p.weight();i++) { weighted.add(p); } } }
        int[][][] result=new int[trials][28][3]; Random random=new Random(seed);
        for(int[][] trial:result) { for(int[] orders:trial) { for(int i=0;i<3;i++) {
            orders[i]=weighted.isEmpty()?-1:weighted.get(random.nextInt(weighted.size())).ordinal()*3+random.nextInt(3);
        } } }
        return result;
    }
    Result evaluate(Input input,int[] counts,int trials) { return evaluate(input,counts,streams(input.level,Math.max(1,trials),234923),0); }
    Result evaluate(Input input,int[] counts,int[][][] streams,int evaluations) {
        EnumMap<Potion,Integer> quantities=new EnumMap<>(Potion.class);
        for(Potion p:Potion.values()) { quantities.put(p,counts[p.ordinal()]); }
        BatchPlan plan=BatchPlan.forInventory(quantities,input.stationOrder,input.inventory);
        int[] ids=new int[plan.size()];
        int k=0; for(BatchEntry entry:plan.getEntries()) { ids[k++]=entry.getPotion().ordinal()*3+entry.getStation().ordinal(); }
        double xp=0,clicks=0; double[] resin=new double[3];
        for(int[][] trial:streams) {
            int[] remaining=new int[30]; for(int id:ids) { remaining[id]++; }
            int left=ids.length;
            for(int[] orders:trial) {
                if(left==0) { break; }
                int matched=0; int[] delivered=new int[3];
                for(int id:orders) { if(id>=0&&remaining[id]>0) { remaining[id]--; delivered[matched++]=id; left--; } }
                clicks++;
                if(matched==0) {
                    for(int id:ids) { if(remaining[id]>0) {
                        remaining[id]--; left--; Potion p=POTIONS[id/3]; xp+=p.xp()/3.0;
                        for(int c=2;c>=0;c--) { if(p.paste(c)>0) { resin[c]+=10; break; } } break;
                    } }
                } else {
                    double bonus=1+.2*(matched-1);
                    for(int i=0;i<matched;i++) {
                        Potion p=POTIONS[delivered[i]/3]; xp+=p.xp();
                        for(int c=0;c<3;c++) { resin[c]+=p.resin(c)*bonus; }
                    }
                }
            }
        }
        xp/=streams.length; clicks/=streams.length;
        for(int c=0;c<3;c++) { resin[c]/=streams.length; }
        int[] lower=new int[counts.length];
        for(InventorySlot slot:input.inventory) {
            if(slot.isPotion()) { lower[slot.getPotion().ordinal()]++; }
        }
        int[] paste=cost(counts,lower);
        double seconds=4.8+sum(counts)*(2.4+input.processingSeconds)+clicks*1.2;
        long max=Math.max(1,Arrays.stream(input.deficit).max().orElse(0));
        boolean hasTarget=Arrays.stream(input.deficit).sum()>0;
        double useful=0;
        for(int c=0;c<3;c++) { useful+=Math.min(resin[c],input.deficit[c])*(input.deficit[c]/(double)max); }
        double bottleneck=Double.POSITIVE_INFINITY, excess=0;
        for(int c=0;c<3;c++) {
            if(input.deficit[c]>0) { bottleneck=Math.min(bottleneck,Math.min(resin[c],input.deficit[c])*max/input.deficit[c]); }
            excess+=Math.max(0,resin[c]-input.deficit[c]);
        }
        if(!hasTarget) { bottleneck=0; }
        double rewardRate=(bottleneck+.05*useful)/seconds, xpRate=xp/30.0/seconds;
        double score;
        switch(input.goal) {
            case REWARDS: score=hasTarget?rewardRate:xpRate; break;
            case BALANCED: score=hasTarget?.5*rewardRate+.5*xpRate:xpRate; break;
            case EFFICIENCY: score=(hasTarget?bottleneck+.1*useful-.05*excess:xp/30)/(Math.max(30,Arrays.stream(paste).sum())+seconds*.1); break;
            default: score=xpRate;
        }
        return new Result(plan,xp,seconds,score,clicks,resin,paste,evaluations);
    }
}
