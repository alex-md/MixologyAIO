package com.mixologybatch;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

public class StationBorderTimerTest
{
    @Test public void borderStaysOnTheSilhouetteAndPreservesGraphicsState() throws Exception {
        BufferedImage image=new BufferedImage(600,180,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(32,35,37));g.fillRect(0,0,600,180);
        Stroke original=g.getStroke();Color originalColor=g.getColor();
        for(int i=0;i<4;i++) {
            Polygon hull=new Polygon(new int[]{55+i*145,90+i*145,112+i*145,102+i*145,43+i*145,33+i*145},
                new int[]{27,27,70,139,139,70},6);
            g.setColor(new Color(48,53,55));g.fill(hull);g.setColor(originalColor);
            StationBorderTimer.draw(g,hull,new double[]{.15,.5,.9,1}[i],i==3);
            assertEquals(original,g.getStroke());assertEquals(originalColor,g.getColor());
            assertEquals(new Color(48,53,55).getRGB(),image.getRGB(70+i*145,80));
        }
        g.dispose();new File("build/reports").mkdirs();ImageIO.write(image,"png",new File("build/reports/border-timer-preview.png"));
        assertFalse(new MixologyBatchConfig(){}.timingBanner());
        assertTrue(new MixologyBatchConfig(){}.alembicBorderTimer());
    }
}
