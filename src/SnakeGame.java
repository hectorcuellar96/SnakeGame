import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Juego de la vivorita (Snake).
 * - Tablero de 32x32 casillas.
 * - Avanza una casilla cada 500 ms.
 * - Controles: W, A, S, D (R para reiniciar).
 * - Si la vivorita choca con una pared y sigue contra ella por más de
 *   500 ms (es decir, en el siguiente avance tampoco puede moverse), pierde.
 */
public class SnakeGame extends JPanel implements ActionListener, KeyListener {

    private static final int GRID = 32;
    private static final int CELL = 20;
    private static final int DELAY_MS = 500;

    private enum Dir {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);
        final int dx, dy;
        Dir(int dx, int dy) { this.dx = dx; this.dy = dy; }
        boolean opposite(Dir o) { return dx == -o.dx && dy == -o.dy; }
    }

    private final Deque<Point> snake = new ArrayDeque<>();
    private final Random random = new Random();
    private final Timer timer = new Timer(DELAY_MS, this);

    private Dir direction;
    private Dir nextDirection;
    private Point food;
    private boolean touchingWall;
    private boolean gameOver;
    private int score;

    public SnakeGame() {
        setPreferredSize(new Dimension(GRID * CELL, GRID * CELL));
        setBackground(new Color(18, 18, 24));
        setFocusable(true);
        addKeyListener(this);
        reset();
        timer.start();
    }

    private void reset() {
        snake.clear();
        int c = GRID / 2;
        snake.addFirst(new Point(c, c));
        snake.addLast(new Point(c - 1, c));
        snake.addLast(new Point(c - 2, c));
        direction = Dir.RIGHT;
        nextDirection = Dir.RIGHT;
        touchingWall = false;
        gameOver = false;
        score = 0;
        placeFood();
        timer.restart();
        repaint();
    }

    private void placeFood() {
        Point p;
        do {
            p = new Point(random.nextInt(GRID), random.nextInt(GRID));
        } while (snake.contains(p));
        food = p;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        direction = nextDirection;
        Point head = snake.peekFirst();
        int nx = head.x + direction.dx;
        int ny = head.y + direction.dy;

        // Choque con pared
        if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID) {
            if (touchingWall) {
                // Ya llevaba un intervalo completo (500 ms) contra la pared
                endGame();
            } else {
                touchingWall = true; // se queda en su sitio, el jugador tiene 500 ms para girar
            }
            repaint();
            return;
        }
        touchingWall = false;

        Point newHead = new Point(nx, ny);
        boolean eating = newHead.equals(food);

        // Si no come, la cola se libera al avanzar, por lo que no cuenta como choque
        Point tail = snake.peekLast();
        if (snake.contains(newHead) && !(newHead.equals(tail) && !eating)) {
            endGame();
            repaint();
            return;
        }

        snake.addFirst(newHead);
        if (eating) {
            score++;
            if (snake.size() == GRID * GRID) {
                endGame();
            } else {
                placeFood();
            }
        } else {
            snake.removeLast();
        }
        repaint();
    }

    private void endGame() {
        gameOver = true;
        timer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Cuadrícula
        g2.setColor(new Color(30, 30, 40));
        for (int i = 0; i <= GRID; i++) {
            g2.drawLine(i * CELL, 0, i * CELL, GRID * CELL);
            g2.drawLine(0, i * CELL, GRID * CELL, i * CELL);
        }

        // Comida
        g2.setColor(new Color(230, 70, 70));
        g2.fillOval(food.x * CELL + 3, food.y * CELL + 3, CELL - 6, CELL - 6);

        // Vivorita
        boolean first = true;
        for (Point p : snake) {
            if (first) {
                g2.setColor(touchingWall ? new Color(255, 170, 60) : new Color(120, 255, 140));
                first = false;
            } else {
                g2.setColor(new Color(60, 190, 90));
            }
            g2.fillRoundRect(p.x * CELL + 1, p.y * CELL + 1, CELL - 2, CELL - 2, 6, 6);
        }

        // Puntaje
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("Puntos: " + score, 8, 18);

        if (gameOver) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 40));
            drawCentered(g2, "Juego terminado", getHeight() / 2 - 10);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            drawCentered(g2, "Puntos: " + score + "  -  Presiona R para reiniciar", getHeight() / 2 + 30);
        }
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> turn(Dir.UP);
            case KeyEvent.VK_S -> turn(Dir.DOWN);
            case KeyEvent.VK_A -> turn(Dir.LEFT);
            case KeyEvent.VK_D -> turn(Dir.RIGHT);
            case KeyEvent.VK_R -> { if (gameOver) reset(); }
            default -> { }
        }
    }

    private void turn(Dir d) {
        // No se permite dar la vuelta de 180 grados
        if (!d.opposite(direction)) {
            nextDirection = d;
        }
    }

    @Override public void keyReleased(KeyEvent e) { }
    @Override public void keyTyped(KeyEvent e) { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("La vivorita");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            SnakeGame game = new SnakeGame();
            frame.add(game);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}