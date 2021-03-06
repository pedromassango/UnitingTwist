package ru.litun.unitingtwist;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Litun on 07.04.2016.
 */
public class GameField implements Drawable {
    Random random = new Random();
    final List<FlyingGameHexagon> flyingHexagons = new ArrayList<>();
    public static final int DIM = 9;
    FieldGraph graph = new FieldGraph(DIM);

    public GameField() {
        GraphGameHexagon center = graph.getCenter();
        GameHexagon hexagon = new GameHexagon(center.getPoint());
        center.setHexagon(hexagon);
        graph.put(center);
    }

    public void setGameListener(GameListener listener) {
        graph.setGameListener(listener);
    }

    @Override
    public void update(long deltaTime) {
        float k = deltaTime / 1000f;

        for (int i = 0; i < flyingHexagons.size(); i++) {
            FlyingGameHexagon hexagon = flyingHexagons.get(i);
            hexagon.update(deltaTime);
        }
        collisionDetect();
    }

    @Override
    public void draw(float[] mvpMatrix) {
        graph.draw(mvpMatrix);

        for (int i = 0; i < flyingHexagons.size(); i++) {
            FlyingGameHexagon hex = flyingHexagons.get(i);
            GameHexagon h = hex.getHexagon();
            h.draw(mvpMatrix);
        }
    }

    public void newUp(float x, float y) {
        double atan = Math.atan2(y, x);
        float angle = (float) (atan / Math.PI * 180);
        graph.rotate(angle);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Timer timer = new Timer();
    private double previousAngle = 0f;
    private double velocity = 0.1;

    private TimerTask newGenerator() {
        return new TimerTask() {
            @Override
            public void run() {
                //TODO: improve regeneration
                double angle = (float) Math.PI * random.nextFloat() * 2;
                while (Math.abs(previousAngle - angle) < 1)
                    angle = (float) Math.PI * random.nextFloat() * 2;
                previousAngle = angle;

                double x = Math.cos(angle);
                double y = Math.sin(angle);

                double k = 2 / Math.sqrt(x * x + y * y);
                Point point = new Point((float) (x * k), (float) (y * k), 0f);
                GameHexagon gameHexagon = new GameHexagon(point);
                gameHexagon.setColor(random.nextInt(ColorUtils.colorsCount() - 1) + 1);
                final FlyingGameHexagon flyingHexagon = new FlyingGameHexagon(gameHexagon);
                flyingHexagon.setVector((float) -(x * k * velocity), (float) -(y * k * velocity));
                velocity += 0.005;

                //run on ui thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        flyingHexagons.add(flyingHexagon);
                    }
                });
            }
        };
    }

    public void startGenerating() {
        timer = new Timer();
        timer.scheduleAtFixedRate(newGenerator(), 0, 3000);
    }

    public void stopGenerating() {
        timer.cancel();
    }

    private void collisionDetect() {
        List<GraphGameHexagon> endpoints = graph.getEndpoints();
        for (int i = 0; i < endpoints.size(); i++) {
            GraphGameHexagon graphHexagon = endpoints.get(i);
            for (int j = 0; j < flyingHexagons.size(); j++) {
                FlyingGameHexagon flyingHexagon = flyingHexagons.get(j);
                float distance = graphHexagon.distance(flyingHexagon.getPoint());
                if (distance < 0.1f) {
                    findOpen(flyingHexagon);
                    return;
                }
            }
        }
    }

    private void findOpen(FlyingGameHexagon flyingHexagon) {
        float minDistance = Float.MAX_VALUE;
        GraphGameHexagon newHexagon = null;
        for (GraphGameHexagon graphOpened : graph.getOpened()) {
            float distance = graphOpened.distance(flyingHexagon.getPoint());
            if (newHexagon == null) {
                newHexagon = graphOpened;
                minDistance = distance;
            } else if (distance < minDistance) {
                minDistance = distance;
                newHexagon = graphOpened;
            }
        }

        if (newHexagon != null) {
            newHexagon.setHexagon(flyingHexagon.getHexagon());
            graph.put(newHexagon);
            flyingHexagons.remove(flyingHexagon);
        }
    }
}
