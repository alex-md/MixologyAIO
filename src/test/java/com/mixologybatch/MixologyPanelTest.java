package com.mixologybatch;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class MixologyPanelTest
{
    @Test public void sidebarRendersAtRuneLiteWidth() throws Exception {
        MixologyPanel[] panel = new MixologyPanel[1];
        SwingUtilities.invokeAndWait(() -> { net.runelite.client.ui.laf.RuneLiteLAF.setup(); panel[0] = new MixologyPanel(null, new MixologyBatchConfig() {}); });
        java.util.EnumMap<Potion,Integer> counts = new java.util.EnumMap<>(Potion.class);
        counts.put(Potion.MAL,7); counts.put(Potion.MMA,3); counts.put(Potion.MML,6); counts.put(Potion.MLL,6); counts.put(Potion.ALL,6);
        BatchPlan batch = BatchPlan.create(counts,StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);
        FullInventoryOptimizer.Result estimate = new FullInventoryOptimizer.Result(batch,7000,590,1,19,new double[]{240,260,350},new int[]{250,230,360},128);
        panel[0].update(81,new int[]{5000,7000,3000},new int[]{2800,1700,2300},new long[]{13800,11200,15100},estimate,batch,
            Guidance.mixing(new BatchEntry(Potion.MAL,Station.CRYSTALLISE,0,0,0,1),MixStep.resolve(Potion.MAL,new int[3],false)),false,OptimizationGoal.BALANCED);
        SwingUtilities.invokeAndWait(() -> {
            JPanelLayout.layout(panel[0],225,850);
            BufferedImage image = new BufferedImage(225,850,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); panel[0].printAll(g); g.dispose();
            try { new File("build/reports").mkdirs(); ImageIO.write(image,"png",new File("build/reports/sidebar-preview.png")); }
            catch (java.io.IOException ex) { throw new RuntimeException(ex); }
            assertEquals(225,panel[0].getWidth());
            javax.swing.JToggleButton expand = find(panel[0], javax.swing.JToggleButton.class);
            assertNotNull(expand); expand.doClick();
            javax.swing.JTextField search = find(panel[0], javax.swing.JTextField.class);
            assertNotNull(search); search.setText("pouch");
            JPanelLayout.layout(panel[0],225,950);
            BufferedImage filtered = new BufferedImage(225,950,BufferedImage.TYPE_INT_RGB);
            Graphics2D filteredGraphics = filtered.createGraphics(); panel[0].printAll(filteredGraphics); filteredGraphics.dispose();
            try { ImageIO.write(filtered,"png",new File("build/reports/sidebar-search-preview.png")); }
            catch (java.io.IOException ex) { throw new RuntimeException(ex); }
        });
    }
    private static <T> T find(Container root, Class<T> type) {
        for (java.awt.Component child : root.getComponents()) {
            if (type.isInstance(child)) { return type.cast(child); }
            if (child instanceof Container) { T result = find((Container)child,type); if (result != null) { return result; } }
        }
        return null;
    }
    private static class JPanelLayout {
        static void layout(Container c,int width,int height) { c.setSize(width,height); c.doLayout(); descend(c); }
        static void descend(Container c) { c.doLayout(); for (java.awt.Component child:c.getComponents()) { if (child instanceof Container) { descend((Container)child); } } }
    }
}
