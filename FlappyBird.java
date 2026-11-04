import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import java.util.prefs.Preferences;
import javax.sound.sampled.*; // добавлено
import java.io.IOException;
import java.net.URL;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {

    private int boardWidth, boardHeight;

    enum GameState {MENU, TRANSITION, GAME}

    GameState state = GameState.MENU;

    float fadeAlpha = 1.0f;
    Timer fadeTimer;

    Image birdImg, topPipeImg, bottomPipeImg, coinImg, explosionImg;
    Image[] backgrounds;
    Image currentBackground;

    int selectedLevel = 0;
    int[] levelSpeeds;
    int[] levelOpenings;
    int[] pipeIntervals;

    int birdX, birdWidth = 53, birdHeight = 40;
    int highScore = 0;
    boolean showGameOverPanel = false;
    float gameOverAlpha = 0f;

    class Bird {
        int x, y, width, height;
        Image img;

        Bird(Image img, int x, int y, int width, int height) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    class Pipe {
        int x, y, width, height;
        boolean passed = false;
        boolean isTop;

        Pipe(int x, int y, int width, int height, boolean isTop) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.isTop = isTop;
        }
    }

    class Coin {
        int x, y, size;
        boolean collected = false;
        double rotation = 0;

        Coin(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }

    Bird bird;
    int velocityX = -4, velocityY = 0, gravity = 1;
    ArrayList<Pipe> pipes = new ArrayList<>();
    ArrayList<Coin> coins = new ArrayList<>();
    Random random = new Random();

    Timer gameLoop = new Timer(1000 / 60, this);
    Timer placePipeTimer;
    Timer autoReturnTimer;
    boolean gameOver = false;
    double score = 0;

    JButton level1Button, level2Button, level3Button;
    boolean showExplosion = false;
    int explosionX = 0, explosionY = 0;
    boolean birdFallingAfterExplosion = false;
    Clip jumpSound;
    Clip coinSound;

    public FlappyBird(int width, int height) {
        this.boardWidth = width;
        this.boardHeight = height;
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);

        loadImages();
        initLevels();
        createButtons();
        loadHighScore();
        currentBackground = backgrounds[0];
        loadSounds(); // загрузить звуки
    }


    private Clip hitSound;

    private void loadSounds() {
        jumpSound = loadClip("/jump.wav");
        coinSound = loadClip("/coin.wav");
        hitSound = loadClip("/hit.wav");
    }

    private Clip loadClip(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("Не найден звук: " + path);
                return null;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void playClip(Clip clip) {
        if (clip == null) return;
        if (clip.isRunning())
            clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }


    @Override
    public void keyPressed(KeyEvent e) {
        if (state == GameState.GAME && e.getKeyCode() == KeyEvent.VK_SPACE && !gameOver) {
            velocityY = -9;
            if (jumpSound != null) {
                if (jumpSound.isRunning()) jumpSound.stop();
                jumpSound.setFramePosition(0);
                jumpSound.start();
            }
        }
    }

    private void loadHighScore() {
        try {
            Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
            highScore = prefs.getInt("highScore", 0);
        } catch (Exception e) {
            highScore = 0;
        }
    }

    private void saveHighScore() {
        try {
            Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
            prefs.putInt("highScore", highScore);
        } catch (Exception ignored) {
        }
    }

    private void loadImages() {

        birdImg = new ImageIcon(getClass().getResource("/flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("/toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("/bottompipe.png")).getImage();
        coinImg = new ImageIcon(getClass().getResource("/coin.png")).getImage();
        explosionImg = new ImageIcon(getClass().getResource("/explosion.png")).getImage();
        backgrounds = new Image[]{
                new ImageIcon(getClass().getResource("/flappybirdbg.png")).getImage(),
                new ImageIcon(getClass().getResource("/nightbg.png")).getImage(),
                new ImageIcon(getClass().getResource("/forestbg.png")).getImage()
        };
    }

    private void initLevels() {
        birdX = boardWidth / 8;
        levelSpeeds = new int[]{-4, -5, -6};
        levelOpenings = new int[]{boardHeight / 4, boardHeight / 5, boardHeight / 6};
        pipeIntervals = new int[]{1500, 1200, 900};
    }

    private void createButtons() {
        int buttonWidth = boardWidth / 4;
        int buttonHeight = boardHeight / 14;
        int spacing = 15;
        int totalWidth = buttonWidth * 3 + spacing * 2;
        int startX = (boardWidth - totalWidth) / 2;
        int buttonY = boardHeight / 2;

        level1Button = createButton("Уровень 1", startX, buttonY, 0);
        level2Button = createButton("Уровень 2", startX + buttonWidth + spacing, buttonY, 1);
        level3Button = createButton("Уровень 3", startX + 2 * (buttonWidth + spacing), buttonY, 2);

        level1Button.setBounds(startX, buttonY, buttonWidth, buttonHeight);
        level2Button.setBounds(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight);
        level3Button.setBounds(startX + 2 * (buttonWidth + spacing), buttonY, buttonWidth, buttonHeight);

        add(level1Button);
        add(level2Button);
        add(level3Button);
    }

    private JButton createButton(String text, int x, int y, int level) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBounds(x, y, 140, 50);
        button.addActionListener(e -> startTransition(level));
        return button;
    }

    void startTransition(int level) {
        selectedLevel = level;
        currentBackground = backgrounds[level];
        velocityX = levelSpeeds[level];

        level1Button.setVisible(false);
        level2Button.setVisible(false);
        level3Button.setVisible(false);

        if (fadeTimer != null) fadeTimer.stop();

        state = GameState.TRANSITION;
        fadeAlpha = 1.0f;

        fadeTimer = new Timer(30, e -> {
            fadeAlpha -= 0.05f;
            if (fadeAlpha <= 0) {
                fadeAlpha = 0;
                fadeTimer.stop();
                startGame();
            }
            repaint();
        });
        fadeTimer.start();
    }

    void startGame() {

        if (placePipeTimer != null) {
            placePipeTimer.stop();
            placePipeTimer = null;
        }
        if (gameLoop.isRunning()) {
            gameLoop.stop();
        }


        int startY;
        switch (selectedLevel) {
            case 0 -> startY = 150;
            case 1 -> startY = 120;
            case 2 -> startY = 100;
            default -> startY = 160;
        }

        bird = new Bird(birdImg, birdX, startY, birdWidth, birdHeight);
        velocityY = 0;
        gravity = 1;

        pipes.clear();
        coins.clear();
        score = 0;
        gameOver = false;
        state = GameState.GAME;
        showExplosion = false;
        birdFallingAfterExplosion = false;
        showGameOverPanel = false;
        gameOverAlpha = 0f;

        repaint();

        Timer delayedStart = new Timer(700, null);
        delayedStart.addActionListener(e -> {
            delayedStart.stop();
            gameLoop.start();
            placePipeTimer = new Timer(pipeIntervals[selectedLevel], ev -> placePipes());
            placePipeTimer.start();
        });
        delayedStart.setRepeats(false);
        delayedStart.start();
    }

    void returnToMenu() {
        if ((int) score > highScore) {
            highScore = (int) score;
            saveHighScore();
        }

        gameOver = false;
        state = GameState.MENU;
        gameLoop.stop();
        if (placePipeTimer != null) placePipeTimer.stop();
        if (autoReturnTimer != null) autoReturnTimer.stop();
        if (fadeTimer != null) fadeTimer.stop();

        level1Button.setVisible(true);
        level2Button.setVisible(true);
        level3Button.setVisible(true);

        pipes.clear();
        coins.clear();
        bird = null;
        score = 0;
        showExplosion = false;
        birdFallingAfterExplosion = false;
        showGameOverPanel = false;
        gameOverAlpha = 0f;

        repaint();
    }

    void placePipes() {
        if (state != GameState.GAME) return;

        int opening = Math.max(levelOpenings[selectedLevel], 120);
        int topHeight = 100 + random.nextInt(250);
        int bottomY = topHeight + opening;
        int bottomHeight = boardHeight - bottomY;

        pipes.add(new Pipe(boardWidth, 0, 64, topHeight, true));
        pipes.add(new Pipe(boardWidth, bottomY, 64, bottomHeight, false));

        int coinY = topHeight + opening / 2 - 16;
        coins.add(new Coin(boardWidth + 20, coinY, 32));
    }

    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    boolean collisionCoin(Bird a, Coin c) {
        return a.x < c.x + c.size && a.x + a.width > c.x && a.y < c.y + c.size && a.y + a.height > c.y;
    }

    public void move() {
        if (bird == null) return;

        if (!gameOver || birdFallingAfterExplosion) {
            velocityY += gravity;
            bird.y += velocityY;
        }

        bird.y = Math.max(bird.y, 0);

        ArrayList<Pipe> toRemove = new ArrayList<>();

        for (Pipe pipe : pipes) {
            pipe.x += velocityX;

            // Начисляем очки, если птица прошла трубу
            if (!pipe.passed && !gameOver && bird.x > pipe.x + pipe.width) {
                score += 0.5;
                pipe.passed = true;
            }

            // Проверка столкновения с трубой
            if (!gameOver && collision(bird, pipe)) {
                explosionX = bird.x - 16;
                explosionY = bird.y - 16;
                showExplosion = true;
                gameOver = true;
                birdFallingAfterExplosion = true;

                playClip(hitSound);

                if ((int) score > highScore) {
                    highScore = (int) score;
                    saveHighScore();
                }

                showGameOverPanel = true;
                gameOverAlpha = 0.0f;
            }

            if (pipe.x + pipe.width < 0) toRemove.add(pipe);
        }

        pipes.removeAll(toRemove);

        for (Coin coin : coins) {
            coin.x += velocityX;
            coin.rotation += 5;
            if (!coin.collected && !gameOver && collisionCoin(bird, coin)) {
                coin.collected = true;
                score += 1;
                playClip(coinSound);
            }
        }

        // Переход на следующий уровень при достижении 15 очков (кроме 3 уровня)
        if (!gameOver && selectedLevel < 2 && score >= 15) {
            startLevelTransition(selectedLevel + 1);
            return;
        }

        if (bird.y > boardHeight) {
            bird.y = boardHeight + 100;
            birdFallingAfterExplosion = false;
            autoReturnTimer = new Timer(3000, e -> {
                showGameOverPanel = false;
                returnToMenu();
            });
            autoReturnTimer.setRepeats(false);
            autoReturnTimer.start();
        }

}

    void startLevelTransition(int nextLevel) {
        state = GameState.TRANSITION;
        fadeAlpha = 1.0f;
        level1Button.setVisible(false);
        level2Button.setVisible(false);
        level3Button.setVisible(false);

        fadeTimer = new Timer(30, e -> {
            fadeAlpha -= 0.05f;
            if (fadeAlpha <= 0) {
                fadeAlpha = 0;
                fadeTimer.stop();
                selectedLevel = nextLevel;
                currentBackground = backgrounds[selectedLevel];
                velocityX = levelSpeeds[selectedLevel];
                startGame();
            }
            repaint();
        });
        fadeTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw((Graphics2D) g);
    }

    void draw(Graphics2D g) {
        // Рисуем фон
        if (currentBackground != null)
            g.drawImage(currentBackground, 0, 0, boardWidth, boardHeight, null);

        if (state == GameState.MENU) {
            g.setColor(Color.WHITE);


            g.setFont(new Font("Arial", Font.PLAIN, 18));
            String controls = "Управление: Пробел — прыжок";
            int controlsWidth = g.getFontMetrics().stringWidth(controls);
            g.drawString(controls, (boardWidth - controlsWidth) / 2, 30);

            // Заголовок
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String title = "Flappy Bird";
            int titleWidth = g.getFontMetrics().stringWidth(title);
            g.drawString(title, (boardWidth - titleWidth) / 2, boardHeight / 3);

            //выбрать уровень
            g.setFont(new Font("Arial", Font.BOLD, 24));
            String prompt = "Выберите уровень";
            int promptWidth = g.getFontMetrics().stringWidth(prompt);
            g.drawString(prompt, (boardWidth - promptWidth) / 2, boardHeight / 2 - 70);


            g.setFont(new Font("Arial", Font.PLAIN, 18));
            String authors = "Разработчики: vaiww";
            int authorsWidth = g.getFontMetrics().stringWidth(authors);
            g.drawString(authors, (boardWidth - authorsWidth) / 2, boardHeight - 40);
        }



        if ((state == GameState.GAME || state == GameState.TRANSITION) && bird != null) {
            // Рисуем птицу
            g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);

            // Рисуем трубы
            for (Pipe pipe : pipes) {
                Image img = pipe.isTop ? topPipeImg : bottomPipeImg;
                g.drawImage(img, pipe.x, pipe.y, pipe.width, pipe.height, null);
            }

            // Рисуем монеты с вращением
            for (Coin coin : coins) {
                if (!coin.collected) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    AffineTransform old = g2d.getTransform();
                    g2d.rotate(Math.toRadians(coin.rotation), coin.x + coin.size / 2, coin.y + coin.size / 2);
                    g2d.drawImage(coinImg, coin.x, coin.y, coin.size, coin.size, null);
                    g2d.setTransform(old);
                    g2d.dispose();
                }
            }

            // Взрыв при столкновении
            if (showExplosion) {
                g.drawImage(explosionImg, explosionX, explosionY, 64, 64, null);
            }

            // Отображение счета
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.drawString(gameOver ? "Game Over: " + (int) score : String.valueOf((int) score), 10, 35);
        }

        // Затухание при переходе уровней
        if (state == GameState.TRANSITION && fadeAlpha > 0) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, boardWidth, boardHeight);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
        }

        // Анимированное окно Game Over
        if (showGameOverPanel) {
            if (gameOverAlpha < 1f) {
                gameOverAlpha += 0.02f;
                if (gameOverAlpha > 1f) gameOverAlpha = 1f;
            }

            Composite oldComp = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, gameOverAlpha));
            g.setColor(new Color(0, 0, 0, 200));
            int panelWidth = 400, panelHeight = 200;
            int panelX = (boardWidth - panelWidth) / 2;
            int panelY = (boardHeight - panelHeight) / 2;
            g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            String text = "Game Over";
            int textWidth = g.getFontMetrics().stringWidth(text);
            g.drawString(text, panelX + (panelWidth - textWidth) / 2, panelY + 60);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String scoreText = "Score: " + (int) score;
            String bestText = "Best: " + highScore;
            int scoreWidth = g.getFontMetrics().stringWidth(scoreText);
            int bestWidth = g.getFontMetrics().stringWidth(bestText);
            g.drawString(scoreText, panelX + (panelWidth - scoreWidth) / 2, panelY + 110);
            g.drawString(bestText, panelX + (panelWidth - bestWidth) / 2, panelY + 150);

            g.setComposite(oldComp);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.GAME) {
            move();
            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}
}
