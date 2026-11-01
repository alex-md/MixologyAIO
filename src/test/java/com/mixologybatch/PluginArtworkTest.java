package com.mixologybatch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import net.runelite.client.util.ImageUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class PluginArtworkTest
{
    @Test public void hubIconAndBundledSidebarArtworkAreUsable() throws Exception {
        BufferedImage hub = ImageIO.read(new File("icon.png"));
        assertNotNull(hub); assertTrue(hub.getWidth() <= 48); assertTrue(hub.getHeight() <= 72);
        assertTrue(hub.getColorModel().hasAlpha());
        BufferedImage bundled = ImageUtil.loadImageResource(MixologyBatchPlugin.class,"/mixology-aio.png");
        assertEquals(hub.getWidth(),bundled.getWidth()); assertEquals(hub.getHeight(),bundled.getHeight());
        for (int y=0;y<hub.getHeight();y++) { for (int x=0;x<hub.getWidth();x++) { assertEquals(hub.getRGB(x,y),bundled.getRGB(x,y)); } }
        BufferedImage sidebar = ImageUtil.resizeImage(bundled,16,16);
        assertEquals(16,sidebar.getWidth()); assertEquals(16,sidebar.getHeight());
        assertNotNull(MixologyBatchPlugin.class.getResourceAsStream("/META-INF/LICENSE.brains-off"));
        assertNotNull(MixologyBatchPlugin.class.getResourceAsStream("/META-INF/LICENSE.mastering-mixology"));
    }
}
