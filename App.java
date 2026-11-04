import javax.swing.*;

public class App {
    public static void main(String[] args) {
        int boardWidth = 480, boardHeight = 800;

        JFrame frame = new JFrame("Flappy Bird");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        FlappyBird flappyBird = new FlappyBird(boardWidth, boardHeight);
        frame.add(flappyBird);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        flappyBird.requestFocusInWindow();
    }
}
