package com.example.connect6demo;



public class GameState {
    private final int[][] board;
    private final int currentPlayer;
    private final String message;
    private final boolean gameStarted;

    public GameState(int[][] board, int currentPlayer, String message, boolean gameStarted) {
        this.board = board;
        this.currentPlayer = currentPlayer;
        this.message = message;
        this.gameStarted = gameStarted;
    }

    public int[][] getBoard() {
        return board;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public String getMessage() {
        return message;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }
}

