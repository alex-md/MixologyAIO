package com.mixologybatch;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A thin clockwise trace following the station silhouette, with no extra UI box. */
final class StationBorderTimer
{
    static final Color READY = new Color(125, 211, 149, 225);
    static Color color(double progress) {
        double t = Math.max(0, Math.min(1, (progress - .55) / .4));
        return new Color((int)(102 + 111*t), (int)(165 + 10*t), (int)(174 - 74*t), 210);
    }
    static void draw(Graphics2D graphics, Shape hull, double progress, boolean ready) {
        if (hull == null || (!ready && progress < 0)) { return; }
        List<Point2D.Double> points = vertices(hull);
        if (points.size() < 3) { return; }
        double area = 0;
        for (int i=0;i<points.size();i++) {
            Point2D a=points.get(i), b=points.get((i+1)%points.size()); area += a.getX()*b.getY()-b.getX()*a.getY();
        }
        if (area < 0) { Collections.reverse(points); }
        int top=0;
        for(int i=1;i<points.size();i++) { if(points.get(i).y<points.get(top).y) { top=i; } }
        Collections.rotate(points,-top);
        double perimeter=0;
        for(int i=0;i<points.size();i++) { perimeter+=points.get(i).distance(points.get((i+1)%points.size())); }
        Path2D base=new Path2D.Double(); base.moveTo(points.get(0).x,points.get(0).y);
        for(int i=1;i<points.size();i++) { base.lineTo(points.get(i).x,points.get(i).y); } base.closePath();
        double remaining=perimeter*(ready?1:Math.max(0,Math.min(1,progress)));
        Path2D trace=new Path2D.Double(); trace.moveTo(points.get(0).x,points.get(0).y);
        for(int i=0;i<points.size()&&remaining>0;i++) {
            Point2D.Double a=points.get(i), b=points.get((i+1)%points.size()); double length=a.distance(b);
            if(length==0) { continue; }
            double t=Math.min(1,remaining/length); trace.lineTo(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t); remaining-=length;
        }
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.setColor(new Color(100,125,130,65)); g.draw(base);
            g.setColor(ready?READY:color(progress)); g.draw(trace);
        } finally { g.dispose(); }
    }
    private static List<Point2D.Double> vertices(Shape shape) {
        List<Point2D.Double> result=new ArrayList<>(); double[] p=new double[6];
        PathIterator it=shape.getPathIterator(null,.75);
        while(!it.isDone()) {
            int type=it.currentSegment(p);
            if(type==PathIterator.SEG_MOVETO && !result.isEmpty()) { break; }
            if(type==PathIterator.SEG_MOVETO||type==PathIterator.SEG_LINETO) { result.add(new Point2D.Double(p[0],p[1])); }
            it.next();
        }
        return result;
    }
}
