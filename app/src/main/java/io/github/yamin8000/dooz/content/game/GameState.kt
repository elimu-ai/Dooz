/*
 *     Dooz
 *     GameState.kt Created by Yamin Siahmargooei at 2022/8/26
 *     This file is part of Dooz.
 *     Copyright (C) 2022  Yamin Siahmargooei
 *
 *     Dooz is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Dooz is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Dooz.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.yamin8000.dooz.content.game

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import io.github.yamin8000.dooz.R
import io.github.yamin8000.dooz.content.settings
import io.github.yamin8000.dooz.game.GameConstants.gameDefaultSize
import io.github.yamin8000.dooz.game.logic.GameLogic
import io.github.yamin8000.dooz.game.logic.SimpleGameLogic
import io.github.yamin8000.dooz.model.*
import io.github.yamin8000.dooz.ui.RingShape
import io.github.yamin8000.dooz.ui.XShape
import io.github.yamin8000.dooz.ui.toName
import io.github.yamin8000.dooz.ui.toShape
import io.github.yamin8000.dooz.util.Constants
import io.github.yamin8000.dooz.util.DataStoreHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextInt

class GameState(
    private val context: Context,
    private val coroutineScope: LifecycleCoroutineScope,
    var gameCells: MutableState<List<List<DoozCell>>>,
    val gameSize: MutableState<Int>,
    var currentPlayer: MutableState<Player?>,
    var players: MutableState<List<Player>>,
    var gamePlayersType: MutableState<GamePlayersType>,
    var isGameStarted: MutableState<Boolean>,
    var isGameFinished: MutableState<Boolean>,
    var winner: MutableState<Player?>,
    private var gameType: MutableState<GameType>,
    var isGameDrew: MutableState<Boolean>,
    var winnerCells: MutableState<List<DoozCell>>,
    val aiDifficulty: MutableState<AiDifficulty>,
    val isRollingDices: MutableState<Boolean>
) {
    private var gameLogic: GameLogic? = null
    private val datastore = DataStoreHelper(context.settings)

    init {
        coroutineScope.launch { prepareGame() }
    }

    fun newGame() {
        MediaPlayer.create(context, R.raw.dice).start()
        coroutineScope.launch {
            prepareGame()

            isGameStarted.value = true

            dummyDiceRolling()

            if (isAiTurnToPlay())
                playCellByAi()
        }
    }

    private suspend fun prepareGame() {
        resetGame()
        prepareGameRules()
        preparePlayers()
        prepareGameLogic()
    }

    private fun resetGame() {
        winner.value = null
        isGameFinished.value = false
        isGameStarted.value = false
        isGameDrew.value = false
        gameCells.value = getEmptyBoard()
        winnerCells.value = listOf()
    }

    fun playCell(
        cell: DoozCell
    ) {
        checkIfGameIsFinished()
        changeCellOwner(cell)
        checkIfGameIsFinished()

        if (isAiTurnToPlay())
            playCellByAi()
    }

    private fun playCellByAi() {
        checkIfGameIsFinished()
        val cell = gameLogic?.ai?.play()
        if (cell != null) changeCellOwner(cell)
        checkIfGameIsFinished()
    }

    private fun isAiTurnToPlay(): Boolean {
        return gamePlayersType.value == GamePlayersType.PvC &&
                currentPlayer.value?.type == PlayerType.Computer &&
                !isGameFinished.value &&
                isGameStarted.value
    }

    private suspend fun prepareGameRules() {
        gameSize.value = datastore.getInt(Constants.gameSize) ?: gameDefaultSize
        gamePlayersType.value = GamePlayersType.valueOf(
            datastore.getString(Constants.gamePlayersType) ?: GamePlayersType.PvC.name
        )
        aiDifficulty.value = AiDifficulty.valueOf(
            datastore.getString(Constants.aiDifficulty) ?: AiDifficulty.Easy.name
        )
    }

    private fun prepareGameLogic() {
        when (gameType.value) {
            GameType.Simple -> gameLogic =
                SimpleGameLogic(gameCells.value, gameSize.value, aiDifficulty.value)
        }
    }

    private fun getEmptyBoard(): List<List<DoozCell>> {
        val columns = mutableListOf<List<DoozCell>>()
        for (x in 0 until gameSize.value) {
            val row = mutableListOf<DoozCell>()
            for (y in 0 until gameSize.value)
                row.add(DoozCell(x, y))
            columns.add(row)
        }
        return columns
    }

    private suspend fun preparePlayers() {
        val firstPlayerName =
            datastore.getString(Constants.firstPlayerName) ?: Constants.firstPlayerDefaultName
        val secondPlayerName =
            datastore.getString(Constants.secondPlayerName) ?: Constants.secondPlayerDefaultName

        val firstPlayerShape =
            datastore.getString(Constants.firstPlayerShape)?.toShape() ?: XShape
        val secondPlayerShape =
            datastore.getString(Constants.secondPlayerShape)?.toShape() ?: RingShape

        val firstPlayerDice = Random.nextInt(1..6)
        val secondPlayerDice = Random.nextInt(1..6)

        players.value = buildList {
            add(Player(firstPlayerName, firstPlayerShape.toName(), diceIndex = firstPlayerDice))
            if (gamePlayersType.value == GamePlayersType.PvC) {
                add(
                    Player(
                        name = context.getString(R.string.computer),
                        shape = secondPlayerShape.toName(),
                        type = PlayerType.Computer,
                        diceIndex = secondPlayerDice
                    )
                )
            } else add(
                Player(
                    secondPlayerName,
                    secondPlayerShape.toName(),
                    diceIndex = secondPlayerDice
                )
            )
        }
        currentPlayer.value = players.value.reduce { first, second ->
            if (first.diceIndex >= second.diceIndex) first else second
        }
    }

    private suspend fun dummyDiceRolling() {
        isRollingDices.value = true

        val firstPlayerDice = players.value.first().diceIndex
        val secondPlayerDice = players.value.last().diceIndex

        repeat(5) {
            players.value = buildList {
                add(players.value.first().copy(diceIndex = Random.nextInt(1..6)))
                add(players.value.last().copy(diceIndex = Random.nextInt(1..6)))
            }
            delay(100)
        }
        players.value = buildList {
            add(players.value.first().copy(diceIndex = firstPlayerDice))
            add(players.value.last().copy(diceIndex = secondPlayerDice))
        }
        delay(100)

        delay(500)
        isRollingDices.value = false
    }

    fun getOwnerShape(
        owner: Player?
    ): Shape {
        return if (owner == players.value.first()) owner.shape?.toShape() ?: XShape
        else owner?.shape?.toShape() ?: RingShape
    }

    private fun changePlayer() {
        if (currentPlayer.value == players.value.first()) currentPlayer.value = players.value.last()
        else currentPlayer.value = players.value.first()
    }

    private fun changeCellOwner(
        cell: DoozCell
    ) {
        if (cell.owner == null && isGameStarted.value) {
            cell.owner = currentPlayer.value
            changePlayer()
        }
    }

    private fun checkIfGameIsFinished() {
        winner.value = findWinner()
        if (winner.value != null)
            finishGame()
        if (gameLogic?.isGameDrew() == true)
            handleDrewGame()
    }

    private fun handleDrewGame() {
        finishGame()
        isGameDrew.value = true
    }

    private fun finishGame() {
        isGameFinished.value = true
        winnerCells.value = gameLogic?.winnerCells ?: listOf()
    }

    private fun findWinner(): Player? {
        return when (gameType.value) {
            GameType.Simple -> gameLogic?.findWinner()
        }
    }
}

@Composable
fun rememberHomeState(
    context: Context = LocalContext.current,
    coroutineScope: LifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycleScope,
    doozCells: MutableState<List<List<DoozCell>>> = rememberSaveable { mutableStateOf(emptyList()) },
    gameSize: MutableState<Int> = rememberSaveable { mutableStateOf(gameDefaultSize) },
    currentPlayer: MutableState<Player?> = rememberSaveable { mutableStateOf(null) },
    players: MutableState<List<Player>> = rememberSaveable { mutableStateOf(listOf()) },
    gamePlayersType: MutableState<GamePlayersType> = rememberSaveable {
        mutableStateOf(
            GamePlayersType.PvC
        )
    },
    isGameStarted: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    isGameFinished: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    winner: MutableState<Player?> = rememberSaveable { mutableStateOf(null) },
    gameType: MutableState<GameType> = rememberSaveable { mutableStateOf(GameType.Simple) },
    isGameDrew: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    winnerCells: MutableState<List<DoozCell>> = rememberSaveable { mutableStateOf(emptyList()) },
    aiDifficulty: MutableState<AiDifficulty> = rememberSaveable { mutableStateOf(AiDifficulty.Easy) },
    isRollingDices: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) }
) = remember(
    context,
    coroutineScope,
    doozCells,
    gameSize,
    currentPlayer,
    players,
    gamePlayersType,
    isGameStarted,
    isGameFinished,
    winner,
    gameType,
    isGameDrew,
    winnerCells,
    aiDifficulty,
    isRollingDices
) {
    GameState(
        context,
        coroutineScope,
        doozCells,
        gameSize,
        currentPlayer,
        players,
        gamePlayersType,
        isGameStarted,
        isGameFinished,
        winner,
        gameType,
        isGameDrew,
        winnerCells,
        aiDifficulty,
        isRollingDices
    )
}