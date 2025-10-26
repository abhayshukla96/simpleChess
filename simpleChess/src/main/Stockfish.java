package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Stockfish {
    
    private Process engineProcess;
    private BufferedReader reader;
    private OutputStreamWriter writer;

 

    public void startEngine(String path) {
        try {
 
 
            engineProcess = new ProcessBuilder(path).start();
            
            reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
            writer = new OutputStreamWriter(engineProcess.getOutputStream());
            
            //  Sending UCI command to switch to the Universal Chess Interface mode
            sendCommand("uci");
            
            // 2. Sending isready command and wait for 'readyok' confirmation
            sendCommand("isready");
            
            while(true) {
                String line = reader.readLine();
                if (line == null) {
                    
                    throw new IOException("Stockfish engine quit unexpectedly during startup.");
                }
                if (line.contains("readyok")) break;
            }
            System.out.println("Stockfish engine started successfully.");
            
        } catch (IOException e) {
            // This catches the 'Access is denied' (CreateProcess error=5) or 'input == null!' errors
            System.err.println("Error starting Stockfish engine: " + e.getMessage());
            // It's good practice to set engineProcess to null if startup fails
            engineProcess = null; 
        }
    }
    public void stopEngine() {
        try {
            if (writer != null) {
                sendCommand("quit");
                writer.close();
            }
            if (reader != null) reader.close();
            if (engineProcess != null) engineProcess.destroy();
            System.out.println("Stockfish engine stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendCommand(String command) {
        try {
            writer.write(command + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getBestMove(String fen, int milliseconds) {
        if (engineProcess == null) return "none";
        
        try {
            // Set the board position using FEN
            sendCommand("position fen " + fen);
            // Search for the best move for a specific duration
            sendCommand("go movetime " + milliseconds);
            
            String bestMove = "none";
            String line;
            
            // Read engine output until 'bestmove' is found
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    bestMove = line.split(" ")[1];
                    break;
                }
            }
            return bestMove;
            
        } catch (IOException e) {
            e.printStackTrace();
            return "none";
        }
    }
}
