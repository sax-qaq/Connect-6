package com.example.connect6demo;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/game")
public class GameController {
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    private static final int SIZE = 19; // 棋盘大小
    private int[][] board = new int[SIZE][SIZE]; // 棋盘状态
    private int currentPlayer = 1; // 当前玩家
    private boolean gameEnded = false; // 游戏是否结束
    private boolean gameStarted = false; // 游戏是否开始
    private ConcurrentHashMap<String, Integer> playerColors = new ConcurrentHashMap<>(); // 玩家颜色
    private List<SseEmitter> emitters = new ArrayList<>(); // SSE 连接
    private int moveCount = 0; // 当前轮次的落子数

    private boolean first = true;

    // 处理玩家移动请求
    @PostMapping("/move")
    public synchronized void makeMove(@RequestBody Move move, @RequestParam String playerId) {
        logger.debug("Received move from player: {}", playerId);
        logger.debug("Player colors: {}", playerColors);
        logger.debug("Current player: {}", currentPlayer);
        logger.debug("Game ended: {}", gameEnded);
        logger.debug("Game started: {}", gameStarted);

        if (!gameEnded && gameStarted && playerColors.containsKey(playerId) && playerColors.get(playerId) == currentPlayer && board[move.getX()][move.getY()] == 0) {
            board[move.getX()][move.getY()] = currentPlayer;
            moveCount++;
            boolean win = checkWin(move.getX(), move.getY());

            if (win) {
                gameEnded = true;
            }

            // 如果黑方落子为第一手且已下了一颗，切换到白方
            if (currentPlayer == 1 && moveCount == 1 && first) {
                currentPlayer = 2;
                moveCount = 0;
                first = false;
            } else if (moveCount == 2) { // 每次轮到任何一方的落子数为两颗
                currentPlayer = currentPlayer == 1 ? 2 : 1;
                moveCount = 0;
            }

            GameState gameState = new GameState(board, currentPlayer, win ? "Player " + (currentPlayer == 1 ? 2 : 1) + " wins!" : null, gameStarted);
            emitUpdate(gameState);
        } else {
            logger.debug("Invalid move or not player's turn or game ended or game not started");
        }
    }

    // 获取当前游戏状态
    @GetMapping("/state")
    public GameState getCurrentState() {
        return new GameState(board, currentPlayer, null, gameStarted);
    }

    // SSE 更新
    @GetMapping("/updates")
    public SseEmitter getUpdates(@RequestParam String playerId) {
        logger.debug("New connection from player: {}", playerId);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 防止超时
        emitters.add(emitter);
        emitter.onCompletion(() -> {
            logger.debug("Emitter completed for player: {}", playerId);
            emitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            logger.debug("Emitter timeout for player: {}", playerId);
            emitters.remove(emitter);
        });
        emitter.onError((Throwable t) -> {
            logger.error("Emitter error for player: {}", playerId, t);
            emitters.remove(emitter);
        });
        return emitter;
    }

    // 重置游戏
    @PostMapping("/reset")
    public synchronized void resetGame() {
        logger.debug("Game reset");
        board = new int[SIZE][SIZE];
        currentPlayer = 1;
        gameEnded = false;
        gameStarted = false;
        playerColors.clear();
        moveCount = 0;
        first = true;
        GameState gameState = new GameState(board, currentPlayer, null, gameStarted);
        emitUpdate(gameState); // 向所有客户端发送更新
    }

    // 玩家选择颜色
    @PostMapping("/choose")
    public synchronized void choosePlayer(@RequestBody PlayerChoice choice) {
        logger.debug("Player {} chose color {}", choice.getPlayerId(), choice.getChoice());

        if (!playerColors.containsValue(choice.getChoice())) {
            playerColors.put(choice.getPlayerId(), choice.getChoice());
            logger.debug("Player colors updated: {}", playerColors);
        }

        // 只有当双方都选择了颜色时才开始游戏
        if (playerColors.size() == 2) {
            gameStarted = true;
        }
        GameState gameState = new GameState(board, currentPlayer, null, gameStarted);
        emitUpdate(gameState); // 向所有客户端发送更新
    }

    // 向所有客户端发送更新
    private void emitUpdate(GameState gameState) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().data(gameState));
                } catch (IOException e) {
                    logger.error("Error sending update to emitter", e);
                    deadEmitters.add(emitter);
                }
            } else {
                logger.error("Emitter is null");
            }
        }
        emitters.removeAll(deadEmitters);
        logger.debug("Update emitted to all clients");
    }

    // 检查是否获胜
    private boolean checkWin(int x, int y) {
        int[] dx = {1, 0, 1, 1};
        int[] dy = {0, 1, 1, -1};
        for (int d = 0; d < 4; d++) {
            int count = 1;
            for (int step = 1; step < 6; step++) {
                int nx = x + step * dx[d];
                int ny = y + step * dy[d];
                if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == currentPlayer) {
                    count++;
                } else {
                    break;
                }
            }
            for (int step = 1; step < 6; step++) {
                int nx = x - step * dx[d];
                int ny = y - step * dy[d];
                if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == currentPlayer) {
                    count++;
                } else {
                    break;
                }
            }
            if (count >= 6) {
                return true;
            }
        }
        return false;
    }
}












