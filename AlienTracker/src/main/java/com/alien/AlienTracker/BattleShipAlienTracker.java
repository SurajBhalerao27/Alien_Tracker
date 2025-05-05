package com.alien.AlienTracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

class Ship2 {
    String id;
    Color color;
    List<Point> movement = new ArrayList<>();
    boolean locked = false;
    boolean fired = false;
    Point lastPrediction = null;

    public Ship2(String id, Color color) {
        this.id = id;
        this.color = color;
    }

    public Point predictNext() {
        if (movement.size() < 2) return null;
        Point last = movement.get(movement.size() - 1);
        Point prev = movement.get(movement.size() - 2);
        int dx = last.x - prev.x;
        int dy = last.y - prev.y;
        lastPrediction = new Point(last.x + dx, last.y + dy);
        return lastPrediction;
    }

    public Point getLastPosition() {
        return movement.get(movement.size() - 1);
    }
}

public class BattleShipAlienTracker extends JPanel implements ActionListener {
    private static final int GRID_SIZE = 11;
    private static final int CELL_SIZE = 50;
    private static final int DELAY = 1000;
    private static final int BATTLE_X = GRID_SIZE / 2;
    private static final int BATTLE_Y = GRID_SIZE - 1;

    private final Map<String, Ship2> ships = new LinkedHashMap<>();
    private final Random rand = new Random();
    private final Timer timer = new Timer(DELAY, this);
    private boolean gameOver = false;
    private boolean victory = false;

    private BufferedImage alienImg, predictImg, fireImg, shipImg;

    public BattleShipAlienTracker() {
        // Set a fixed width and dynamically calculated height based on the window's height
        setPreferredSize(new Dimension(GRID_SIZE * CELL_SIZE, (GRID_SIZE * CELL_SIZE) + 50));
        setBackground(Color.BLACK);

        ships.put("AlienA", new Ship2("AlienA", Color.GREEN));
        ships.put("AlienB", new Ship2("AlienB", Color.ORANGE));
        ships.put("AlienC", new Ship2("AlienC", Color.CYAN));

        for (Ship2 ship : ships.values()) {
            int x = rand.nextInt(GRID_SIZE);
            int y = rand.nextInt(GRID_SIZE / 3); // Spawn on upper part
            ship.movement.add(new Point(x, y));
            ship.predictNext();
        }

        try {
            alienImg = ImageIO.read(new File("alien.png"));
            predictImg = ImageIO.read(new File("predict.png"));
            fireImg = ImageIO.read(new File("fire.png"));
            shipImg = ImageIO.read(new File("ship.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.GRAY);
        for (int i = 0; i <= GRID_SIZE; i++) {
            g.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, GRID_SIZE * CELL_SIZE);
            g.drawLine(0, i * CELL_SIZE, GRID_SIZE * CELL_SIZE, i * CELL_SIZE);
        }

        if (shipImg != null) {
            g.drawImage(shipImg, BATTLE_X * CELL_SIZE + 5, BATTLE_Y * CELL_SIZE + 5,
                    CELL_SIZE - 10, CELL_SIZE - 10, null);
        }

        for (Ship2 ship : ships.values()) {
            Point last = ship.getLastPosition();

            if (!ship.fired && alienImg != null) {
                g.drawImage(alienImg, last.x * CELL_SIZE + 5, last.y * CELL_SIZE + 5,
                        CELL_SIZE - 10, CELL_SIZE - 10, null);
            }

            if (!ship.locked && ship.lastPrediction != null && predictImg != null) {
                g.drawImage(predictImg, ship.lastPrediction.x * CELL_SIZE + 10, ship.lastPrediction.y * CELL_SIZE + 10,
                        CELL_SIZE - 20, CELL_SIZE - 20, null);
            }

            if (ship.fired && fireImg != null) {
                g.drawImage(fireImg, last.x * CELL_SIZE + 5, last.y * CELL_SIZE + 5,
                        CELL_SIZE - 10, CELL_SIZE - 10, null);
            }
        }

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 28));
            if (victory) {
                g.setColor(Color.GREEN.brighter());
                g.drawString("VICTORY! All aliens locked!", 80, GRID_SIZE * CELL_SIZE + 40);
            } else {
                g.setColor(Color.RED);
                g.drawString("GAME OVER! Alien reached the ship!", 30, GRID_SIZE * CELL_SIZE + 40);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        for (Ship2 ship : ships.values()) {
            if (ship.fired) continue;

            Point last = ship.getLastPosition();
            List<Point> possibleMoves = new ArrayList<>();

            int[] dxOptions = {-1, 0, 1};
            int[] dyOptions = {-1, 0, 1};

            for (int dx : dxOptions) {
                for (int dy : dyOptions) {
                    if (dx == 0 && dy == 0) continue;
                    int newX = last.x + dx;
                    int newY = last.y + dy;
                    if (newX >= 0 && newX < GRID_SIZE && newY >= 0 && newY < GRID_SIZE) {
                        Point next = new Point(newX, newY);
                        if (!ship.movement.contains(next)) {
                            possibleMoves.add(next);
                        }
                    }
                }
            }

            possibleMoves.sort(Comparator.comparingInt(p -> Math.abs(p.x - BATTLE_X) + Math.abs(p.y - BATTLE_Y)));

            int limit = Math.min(3, possibleMoves.size());
            if (limit > 0) {
                Point chosen = possibleMoves.get(rand.nextInt(limit));
                ship.movement.add(chosen);

                if (ship.lastPrediction != null && ship.lastPrediction.equals(chosen)) {
                    ship.locked = true;
                    ship.fired = true;
                }

                if (chosen.x == BATTLE_X && chosen.y == BATTLE_Y) {
                    gameOver = true;
                    victory = false;
                    timer.stop();
                    repaint();
                    return;
                }

                ship.predictNext();
            }
        }

        if (ships.values().stream().allMatch(s -> s.fired)) {
            gameOver = true;
            victory = true;
            timer.stop();
        }

        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Alien Invasion");
            BattleShipAlienTracker panel = new BattleShipAlienTracker();

            JButton restartButton = new JButton("Start Over");
            restartButton.addActionListener(e -> {
                frame.dispose();
                main(null);
            });

            JPanel container = new JPanel(new BorderLayout());
            container.add(panel, BorderLayout.CENTER);
            container.add(restartButton, BorderLayout.SOUTH);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(container);
            frame.setSize(new Dimension(580, 680)); // Adjust initial size to ensure it fits vertically
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
