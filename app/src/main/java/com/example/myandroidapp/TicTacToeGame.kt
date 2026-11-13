package com.example.myandroidapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class Player { X, O }
private enum class GameState { PLAYING, WIN, DRAW }

private val X_COLOR = Color(0xFF42A5F5)
private val O_COLOR = Color(0xFFEF5350)

@Composable
fun TicTacToeGame() {
    var board by remember { mutableStateOf(List(9) { null as Player? }) }
    var currentPlayer by remember { mutableStateOf(Player.X) }
    var gameState by remember { mutableStateOf(GameState.PLAYING) }
    var winningLine by remember { mutableStateOf<List<Int>?>(null) }
    var scoreX by remember { mutableStateOf(0) }
    var scoreO by remember { mutableStateOf(0) }
    var scoreDraw by remember { mutableStateOf(0) }

    fun resetBoard() {
        board = List(9) { null }
        currentPlayer = Player.X
        gameState = GameState.PLAYING
        winningLine = null
    }

    fun checkWinner(b: List<Player?>): Pair<GameState, List<Int>?> {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (line in lines) {
            val (a, b1, c) = line
            if (b[a] != null && b[a] == b[b1] && b[b1] == b[c]) {
                return GameState.WIN to line
            }
        }
        return if (b.all { it != null }) GameState.DRAW to null else GameState.PLAYING to null
    }

    fun onCellClick(index: Int) {
        if (gameState != GameState.PLAYING || board[index] != null) return
        val newBoard = board.toMutableList()
        newBoard[index] = currentPlayer
        val (state, line) = checkWinner(newBoard)
        board = newBoard
        winningLine = line
        gameState = state
        when (state) {
            GameState.WIN -> if (currentPlayer == Player.X) scoreX++ else scoreO++
            GameState.DRAW -> scoreDraw++
            GameState.PLAYING -> currentPlayer = if (currentPlayer == Player.X) Player.O else Player.X
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tic Tac Toe",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Jetpack Compose",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8)
        )

        Spacer(Modifier.height(20.dp))

        ScoreBoard(scoreX, scoreO, scoreDraw)

        Spacer(Modifier.height(24.dp))

        StatusText(gameState, currentPlayer, board.count { it == null })

        Spacer(Modifier.height(20.dp))

        Board(board = board, winningLine = winningLine, onCellClick = { onCellClick(it) })

        Spacer(Modifier.height(28.dp))

        AnimatedVisibility(visible = gameState != GameState.PLAYING) {
            Button(
                onClick = { resetBoard() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
            ) {
                Text("Play Again", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (gameState == GameState.PLAYING) {
            OutlinedButton(
                onClick = { resetBoard() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
            ) {
                Text("Reset Game", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ScoreBoard(scoreX: Int, scoreO: Int, scoreDraw: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScoreCard("X", scoreX, X_COLOR, Modifier.weight(1f))
        ScoreCard("O", scoreO, O_COLOR, Modifier.weight(1f))
        ScoreCard("Draw", scoreDraw, Color(0xFF94A3B8), Modifier.weight(1f))
    }
}

@Composable
private fun ScoreCard(label: String, score: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(score.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusText(state: GameState, current: Player, movesLeft: Int) {
    val text = when (state) {
        GameState.PLAYING -> "Player ${current.name}'s turn"
        GameState.WIN -> "Player ${current.name} wins! 🎉"
        GameState.DRAW -> "It's a Draw! 🤝"
    }
    val color = when (state) {
        GameState.WIN -> if (current == Player.X) X_COLOR else O_COLOR
        else -> Color.White
    }
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Board(board: List<Player?>, winningLine: List<Int>?, onCellClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    Cell(
                        player = board[index],
                        isWinning = winningLine?.contains(index) == true,
                        onClick = { onCellClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Cell(player: Player?, isWinning: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (player != null) 1f else 0.85f)
    val bg = if (isWinning) Color(0xFF334155) else Color(0xFF1E293B)
    Box(
        modifier = Modifier
            .size(92.dp)
            .scale(scale)
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(enabled = player == null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (player) {
            Player.X -> Text("X", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = X_COLOR)
            Player.O -> Text("O", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = O_COLOR)
            null -> {}
        }
    }
}
