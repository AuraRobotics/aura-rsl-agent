package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class K_LayerTravelCost extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(2));
        g2.setFont(new Font("Arial", 0, 9));
        g2.setColor(Color.white);
	
	wsg.dijkstra(wsg.ai.getPosition());
	
        for(AURAreaGraph ag : wsg.areas.values()) {
            String cost = "inf";
            if(ag.lastDijkstraEntranceNode != null || true) {
                cost = (int) ag.getLastDijkstraCost() + "";
            }

            g2.drawString(cost, kst.xToScreen(ag.getX()),  kst.yToScreen(ag.getY()));

        }
        g2.setStroke(new BasicStroke(1));
    }
    
}
