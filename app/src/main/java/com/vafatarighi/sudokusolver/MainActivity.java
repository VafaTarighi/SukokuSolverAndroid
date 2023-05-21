package com.vafatarighi.sudokusolver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.GameState;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.sfuhrm.sudoku.GameMatrix;
import de.sfuhrm.sudoku.GameMatrixFactory;
import de.sfuhrm.sudoku.GameSchema;
import de.sfuhrm.sudoku.GameSchemas;
import de.sfuhrm.sudoku.QuadraticArrays;
import de.sfuhrm.sudoku.Riddle;
import de.sfuhrm.sudoku.Solver;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_PICKER_REQUEST_CODE = 1;

    private String[] gameBoard = new String[] {
            "000000000", "000000000", "000000000",
            "000000000", "000000000", "000000000",
            "000000000", "000000000", "000000000"
    };

    private Timer gameSolverTimer;
    private boolean isPlaying = false;
    private final EditText[][] gameBoardView = new EditText[9][9];

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameSolverTimer.cancel();
        isPlaying = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadGameBoardView();

        ImageButton load_game_btn = findViewById(R.id.btn_load_game_file);
        load_game_btn.setOnClickListener(view -> {

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            startActivityForResult(intent, FILE_PICKER_REQUEST_CODE);
        });

        ImageButton restart_game_btn = findViewById(R.id.btn_restart_game);
        restart_game_btn.setOnClickListener(view -> {
            resetGameBoard();
        });

        ImageButton pause_play_btn = findViewById(R.id.btn_pause_play_game);
        pause_play_btn.setOnClickListener(view -> {

            if (isPlaying && gameSolverTimer != null) {
                gameSolverTimer.cancel();
                gameSolverTimer = null;
                toggleBoardEnable(true);
                pause_play_btn.setImageResource(R.drawable.play_game_icon);
                return;
            }

            toggleBoardEnable(false);

            byte[][] solvedBoard = solveTheGame();
            if (solvedBoard == null) return;

            gameSolverTimer = new Timer();
            TimerTask task = new TimerTask() {
                private int curr_row = 0;
                private int curr_col = 0;
                @Override
                public void run() {
                   while(true)  {
                       if (gameBoardView[curr_row][curr_col].getText().toString()
                               .equals(String.valueOf(solvedBoard[curr_row][curr_col]))) {
                           curr_col++;
                           if (curr_col % 9 == 0) {
                               curr_col = 0;
                               curr_row++;
                           }
                           if (curr_row == 9) {
                               gameSolverTimer.cancel();
                               pause_play_btn.setImageResource(R.drawable.play_game_icon);
                               return;
                           }
                       } else break;
                   }
                   gameBoardView[curr_row][curr_col]
                            .setText(String.valueOf(solvedBoard[curr_row][curr_col]));
                   gameBoardView[curr_row][curr_col].setTextColor(Color.YELLOW);
                    gameBoardView[curr_row][curr_col].setEnabled(false);
                }
            };
            gameSolverTimer.schedule(task, 500, 500);
            isPlaying = true;
            pause_play_btn.setImageResource(R.drawable.pause_game_icon);
        });


    }

    private void toggleBoardEnable(boolean enable) {
        for(int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                EditText cell = gameBoardView[i][j];

                if (cell.getCurrentTextColor() == Color.BLACK) cell.setEnabled(enable);
            }
        }
    }

    private byte[][] solveTheGame() {
        Riddle riddle = new GameMatrixFactory().newRiddle(GameSchemas.SCHEMA_9X9);
        riddle.setAll(QuadraticArrays.parse(gameBoard));

        if (!riddle.isValid()) {
            Toast.makeText(this, "Unsolvable Game!", Toast.LENGTH_LONG).show();
            return null;
        }

        Solver solver = new Solver(riddle);
        List<GameMatrix> solutions = solver.solve();

        byte[][] solvedBoard = solutions.get(0).getArray();

        return solvedBoard;
    }

    @SuppressLint("SetTextI18n")
    private void resetGameBoard() {
        for (int row = 0; row < 9; row++)
            for (int col = 0; col < 9; col++) {
                if (gameBoard[row].charAt(col) == '0') {
                    gameBoardView[row][col].setText("");
                    gameBoardView[row][col].setEnabled(true);
                    gameBoardView[row][col].setTextColor(Color.BLACK);
                }
                else {
                    gameBoardView[row][col].setText("" + gameBoard[row].charAt(col));
                    gameBoardView[row][col].setEnabled(false);
                }
            }
    }

    private void loadGameBoardView() {
        gameBoardView[0][0] = findViewById(R.id.c1);gameBoardView[0][1] = findViewById(R.id.c2);gameBoardView[0][2] = findViewById(R.id.c3);
        gameBoardView[0][3] = findViewById(R.id.c4);gameBoardView[0][4] = findViewById(R.id.c5);gameBoardView[0][5] = findViewById(R.id.c6);
        gameBoardView[0][6] = findViewById(R.id.c7);gameBoardView[0][7] = findViewById(R.id.c8);gameBoardView[0][8] = findViewById(R.id.c9);
        gameBoardView[1][0] = findViewById(R.id.c10);gameBoardView[1][1] = findViewById(R.id.c11);gameBoardView[1][2] = findViewById(R.id.c12);
        gameBoardView[1][3] = findViewById(R.id.c13);gameBoardView[1][4] = findViewById(R.id.c14);gameBoardView[1][5] = findViewById(R.id.c15);
        gameBoardView[1][6] = findViewById(R.id.c16);gameBoardView[1][7] = findViewById(R.id.c17);gameBoardView[1][8] = findViewById(R.id.c18);
        gameBoardView[2][0] = findViewById(R.id.c19);gameBoardView[2][1] = findViewById(R.id.c20);gameBoardView[2][2] = findViewById(R.id.c21);
        gameBoardView[2][3] = findViewById(R.id.c22);gameBoardView[2][4] = findViewById(R.id.c23);gameBoardView[2][5] = findViewById(R.id.c24);
        gameBoardView[2][6] = findViewById(R.id.c25);gameBoardView[2][7] = findViewById(R.id.c26);gameBoardView[2][8] = findViewById(R.id.c27);
        gameBoardView[3][0] = findViewById(R.id.c28);gameBoardView[3][1] = findViewById(R.id.c29);gameBoardView[3][2] = findViewById(R.id.c30);
        gameBoardView[3][3] = findViewById(R.id.c31);gameBoardView[3][4] = findViewById(R.id.c32);gameBoardView[3][5] = findViewById(R.id.c33);
        gameBoardView[3][6] = findViewById(R.id.c34);gameBoardView[3][7] = findViewById(R.id.c35);gameBoardView[3][8] = findViewById(R.id.c36);
        gameBoardView[4][0] = findViewById(R.id.c37);gameBoardView[4][1] = findViewById(R.id.c38);gameBoardView[4][2] = findViewById(R.id.c39);
        gameBoardView[4][3] = findViewById(R.id.c40);gameBoardView[4][4] = findViewById(R.id.c41);gameBoardView[4][5] = findViewById(R.id.c42);
        gameBoardView[4][6] = findViewById(R.id.c43);gameBoardView[4][7] = findViewById(R.id.c44);gameBoardView[4][8] = findViewById(R.id.c45);
        gameBoardView[5][0] = findViewById(R.id.c46);gameBoardView[5][1] = findViewById(R.id.c47);gameBoardView[5][2] = findViewById(R.id.c48);
        gameBoardView[5][3] = findViewById(R.id.c49);gameBoardView[5][4] = findViewById(R.id.c50);gameBoardView[5][5] = findViewById(R.id.c51);
        gameBoardView[5][6] = findViewById(R.id.c52);gameBoardView[5][7] = findViewById(R.id.c53);gameBoardView[5][8] = findViewById(R.id.c54);
        gameBoardView[6][0] = findViewById(R.id.c55);gameBoardView[6][1] = findViewById(R.id.c56);gameBoardView[6][2] = findViewById(R.id.c57);
        gameBoardView[6][3] = findViewById(R.id.c58);gameBoardView[6][4] = findViewById(R.id.c59);gameBoardView[6][5] = findViewById(R.id.c60);
        gameBoardView[6][6] = findViewById(R.id.c61);gameBoardView[6][7] = findViewById(R.id.c62);gameBoardView[6][8] = findViewById(R.id.c63);
        gameBoardView[7][0] = findViewById(R.id.c64);gameBoardView[7][1] = findViewById(R.id.c65);gameBoardView[7][2] = findViewById(R.id.c66);
        gameBoardView[7][3] = findViewById(R.id.c67);gameBoardView[7][4] = findViewById(R.id.c68);gameBoardView[7][5] = findViewById(R.id.c69);
        gameBoardView[7][6] = findViewById(R.id.c70);gameBoardView[7][7] = findViewById(R.id.c71);gameBoardView[7][8] = findViewById(R.id.c72);
        gameBoardView[8][0] = findViewById(R.id.c73);gameBoardView[8][1] = findViewById(R.id.c74);gameBoardView[8][2] = findViewById(R.id.c75);
        gameBoardView[8][3] = findViewById(R.id.c76);gameBoardView[8][4] = findViewById(R.id.c77);gameBoardView[8][5] = findViewById(R.id.c78);
        gameBoardView[8][6] = findViewById(R.id.c79);gameBoardView[8][7] = findViewById(R.id.c80);gameBoardView[8][8] = findViewById(R.id.c81);

        InputFilter numericFilter = (source, start, end, dest, dStart, dEnd) -> {
            if (end - start == 0) return "";
            if (dest.length() == 1) return "";
            char num = source.charAt(0);
            if (!(num >= '1' && num <= '9')) return "";
            else return String.valueOf(num);
        };

        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                gameBoardView[i][j].setFilters(new InputFilter[]{numericFilter});
            }

    }

    private List<EditText> getAllEditTextViews(ViewGroup viewGroup) {
        List<EditText> editTextList = new ArrayList<>();

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childView = viewGroup.getChildAt(i);

            if (childView instanceof EditText) {
                editTextList.add((EditText) childView);
            } else if (childView instanceof ViewGroup) {
                List<EditText> childEditTextList = getAllEditTextViews((ViewGroup) childView);
                editTextList.addAll(childEditTextList);
            }
        }

        return editTextList;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri game_board_uri = data.getData();
            String filename = getFileNameFromUri(game_board_uri);
            Log.d("FILE NAME --------------", filename);

            if (filename.endsWith(".sdk")) {
                Log.d("FILE EXTENSION CHECK ----------", "passed");
            } else {
                Toast.makeText(this, "File extension not supported!", Toast.LENGTH_LONG).show();
                return;
            }

            String[] board = parseGameBoard(game_board_uri);
            if (board == null) {
                Log.e("GAME BOARD PARSE ERROR", "couldn't parse the game file.");
                Toast.makeText(this, "Couldn't parse game file! check file formatting.", Toast.LENGTH_LONG).show();
            } else {
                this.gameBoard = board;
                resetGameBoard();
            }
        }
    }

    public String getFileNameFromUri(Uri uri) {
        String fileName = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (displayNameIndex != -1) {
                fileName = cursor.getString(displayNameIndex);
            }
            cursor.close();
        }
        return fileName;
    }

    String[] parseGameBoard(Uri game_board_uri) {
        String[] board = new String[9];
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(game_board_uri)))){
            for (int i = 0; i < 9; i++) {
                String line = bis.readLine().replaceAll("\\.", "0");
                if (line.length() != 9)
                    return null;
                for (int j = 0; j < 9; j++) {
                    if ("1234567890".indexOf(line.charAt(j)) == -1)
                        return null;

                }
                board[i] = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return board;
    }
}
