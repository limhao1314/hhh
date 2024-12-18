import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;

public class Game extends JFrame implements MouseListener {
    private final int mapRow = 9;
    private final int mapCol = 9;
    private final JButton[][] buttons = new JButton[mapRow][mapCol]; // Renamed for clarity
    private int minesCount = 10;
    private final JLabel minesLabel = new JLabel("The remaining mines are: " + minesCount);
    private int score = 0;
    private final JLabel scoreLabel = new JLabel("Score: " + score);
    private int elapsedTime = 0;
    private final JLabel timerLabel = new JLabel("Time: " + elapsedTime);
    private Timer timer;
    private final JTextField playerNameField = new JTextField(10);
    private final int[][] map = new int[mapRow][mapCol];
    private final boolean[][] buttonPressed = new boolean[mapRow][mapCol];
    private final int[][] bombsAroundCount = new int[mapRow][mapCol];
    private final int[][] directions = {{0, 0}, {0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {-1, -1}, {-1, 1}, {1, -1}};
    private final ArrayList<PlayerStats> playerStatsList;

    Game() {
        // Window setup
        setSize(850, 850);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Minesweeper");
        setLocationRelativeTo(this);

        // Top bar setup
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Player Name:"));
        topPanel.add(playerNameField);
        topPanel.add(minesLabel);
        topPanel.add(scoreLabel);

        JButton restartButton = new JButton("New Game");
        restartButton.setActionCommand("restart");
        restartButton.addMouseListener(this);
        topPanel.add(restartButton);

        topPanel.add(timerLabel);
        setupTimer();

        // Center button panel setup
        JPanel centerButtonPanel = new JPanel();
        centerButtonPanel.setLayout(new GridLayout(mapRow, mapCol));
        for (int i = 0; i < mapRow; i++) {
            for (int j = 0; j < mapCol; j++) {
                buttons[i][j] = new JButton();
                buttons[i][j].setPreferredSize(new Dimension(80, 80)); // Increase button size
                buttons[i][j].setBackground(Color.WHITE);
                buttons[i][j].setActionCommand(i + " " + j);
                buttons[i][j].addMouseListener(this);
                centerButtonPanel.add(buttons[i][j]);
            }
        }

        add(topPanel, BorderLayout.NORTH);
        add(centerButtonPanel, BorderLayout.CENTER);

        playerStatsList = new ArrayList<>();
        resetGame(); // Initialize the game
        setVisible(true);
    }

    private void setMap() {
        int count = 0;
        while (count < minesCount) {
            int x = (int) (Math.random() * mapRow);
            int y = (int) (Math.random() * mapCol);
            if (map[x][y] == 0) {
                map[x][y] = 1; // Place a mine
                count++;
            }
        }
    }

    private void setupTimer() {
        timer = new Timer(1000, _ -> {
            elapsedTime++;
            timerLabel.setText("Time: " + elapsedTime);
        });
    }

    private void calculateBombsAround() {
        for (int i = 0; i < mapRow; i++) {
            for (int j = 0; j < mapCol; j++) {
                if (map[i][j] == 1) {
                    bombsAroundCount[i][j] = -1;
                } else {
                    int bombCount = 0;
                    for (int[] dir : directions) {
                        int row = i + dir[0];
                        int col = j + dir[1];
                        if (row >= 0 && row < mapRow && col >= 0 && col < mapCol && map[row][col] == 1) {
                            bombCount++;
                        }
                    }
                    bombsAroundCount[i][j] = bombCount;
                }
            }
        }
    }

    private void resetGame() {
        Arrays.stream(map).forEach(row -> Arrays.fill(row, 0));
        Arrays.stream(buttonPressed).forEach(row -> Arrays.fill(row, false));
        Arrays.stream(bombsAroundCount).forEach(row -> Arrays.fill(row, 0));

        minesCount = 10;
        minesLabel.setText("The remaining mines are: " + minesCount);
        score = 0;
        scoreLabel.setText("Score: " + score);
        elapsedTime = 0;
        timerLabel.setText("Time: " + elapsedTime);
        timer.restart();

        playerNameField.setText("");
        setMap();
        calculateBombsAround();

        for (int i = 0; i < mapRow; i++) {
            for (int j = 0; j < mapCol; j++) {
                buttons[i][j].setBackground(Color.WHITE);
                buttons[i][j].setText("");
                buttons[i][j].setIcon(null);
            }
        }
        timer.start(); // Start the timer
    }

    private void revealCell(int row, int col) {
        if (row < 0 || row >= mapRow || col < 0 || col >= mapCol || buttonPressed[row][col]) {
            return;
        }

        // Mark the cell as pressed
        buttonPressed[row][col] = true;
        buttons[row][col].setBackground(Color.GRAY); // Optional: grey out revealed cells

        // Update score and display bomb count if any
        if (bombsAroundCount[row][col] > 0) {
            buttons[row][col].setText(String.valueOf(bombsAroundCount[row][col]));
            score += 5; // Increment score for revealing a number
        } else {
            score += 10; // Increment score for revealing an empty cell
            // Reveal surrounding cells
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0) revealCell(row + i, col + j);
                }
            }
        }

        scoreLabel.setText("Score: " + score);

        // Check for win condition after revealing cell
        if (checkWinCondition()) {
            String playerName = playerNameField.getText();
            storePlayerStats();
            JOptionPane.showMessageDialog(this, "Congratulations! You revealed all non-mine cells!", "You Win!", JOptionPane.INFORMATION_MESSAGE);
            displayAllPlayerStats();
            resetGame();
        }
    }

    private void handleLeftClick(int row, int col) {
        try {
            if (buttonPressed[row][col]) return;

            if (map[row][col] == 1) {
                // Hit a mine!
                buttons[row][col].setBackground(Color.RED);
                showGameOverDialog();
                revealAllMines();
                return;
            }

            revealCell(row, col);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Handle the error gracefully, could log it or display a message
            System.out.println("Invalid cell selection: " + e.getMessage());
        }
    }

    private void revealAllMines() {
        for (int i = 0; i < mapRow; i++) {
            for (int j = 0; j < mapCol; j++) {
                if (map[i][j] == 1) {
                    buttons[i][j].setIcon(new ImageIcon("C:\\path\\to\\mines.jpg")); // Update with proper path
                }
            }
        }
    }

    private void showGameOverDialog() {
        String playerName = playerNameField.getText().isEmpty() ? "Unknown Player" : playerNameField.getText();
        storePlayerStats();
        JOptionPane.showMessageDialog(this, "Game Over! You hit a mine!", "Game Over",	JOptionPane.INFORMATION_MESSAGE);
        displayAllPlayerStats();
        resetGame();
    }

    private void handleRightClick(int row, int col) {
        if (buttonPressed[row][col]) return;

        buttons[row][col].setBackground(Color.GREEN);
        buttonPressed[row][col] = true;
        minesCount--;
        minesLabel.setText("The remaining mines are: " + minesCount);

        // Win check
        if (minesCount == 0 && checkWinCondition()) {
            String playerName = playerNameField.getText();
            storePlayerStats();
            JOptionPane.showMessageDialog(this, "You win!", "Congratulations!", JOptionPane.INFORMATION_MESSAGE);
            displayAllPlayerStats();
            resetGame();
        }
    }

    private boolean checkWinCondition() {
        for (int i = 0; i < mapRow; i++) {
            for (int j = 0; j < mapCol; j++) {
                // Check if a cell is empty and not revealed (not pressed)
                if (map[i][j] == 0 && !buttonPressed[i][j]) {
                    return false; // Found a non-mine cell that is not revealed
                }
            }
        }
        return true; // All non-mine cells have been revealed
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JButton source = (JButton) e.getSource();
        String[] command = source.getActionCommand().split(" ");
        int row = Integer.parseInt(command[0]);
        int col = Integer.parseInt(command[1]);

        if (source.getActionCommand().equals("restart")) {
            resetGame();
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            handleLeftClick(row, col);
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            handleRightClick(row, col);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }


    private void storePlayerStats() {
        String playerName = playerNameField.getText().isEmpty() ? "Unknown Player" : playerNameField.getText();
        playerStatsList.add(new PlayerStats(playerName, score, elapsedTime));
    }

    private void displayAllPlayerStats() {
        if (playerStatsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No player stats available.", "Player Stats", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder statsBuilder = new StringBuilder("Player Stats:\n");
        for (PlayerStats stats : playerStatsList) {
            statsBuilder.append(String.format("Player: %s; Score: %d; Time: %d seconds\n",
                    stats.name(),
                    stats.score(),
                    stats.time()));
        }

        JOptionPane.showMessageDialog(this, statsBuilder.toString(), "Player Stats", JOptionPane.INFORMATION_MESSAGE);
    }
}

record PlayerStats(String name, int score, int time) { }

class Minesweeper {
    public static void main(String[] args) {
        new Game();
    }
}