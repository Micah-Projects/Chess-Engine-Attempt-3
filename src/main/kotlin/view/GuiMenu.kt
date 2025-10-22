package view

import javafx.application.Application
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import model.board.MutableChessBoard

import kotlin.jvm.javaClass

class GuiMenu : Application() {

    @FXML
    private lateinit var stage: Stage
    private lateinit var scene: Scene
    private lateinit var root: Parent


    @FXML
    fun startVsAI(e: ActionEvent) {
       // UI.Config.player1 = player.HumanPlayers.VisualPlayer()
       // UI.Config.player2 = null       // null means random engine
        load(e)
    }

    @FXML
    fun aIVsAI(e: ActionEvent) {
       // UI.Config.player1 = engines.MasterOfZeroDepth()
       // UI.Config.player2 = engines.MasterOfZeroDepth()
        load(e)
    }

    @FXML
    fun playerVsPlayer(e: ActionEvent) {
        //UI.Config.player1 = player.HumanPlayers.VisualPlayer()
       // UI.Config.player2 = player.HumanPlayers.VisualPlayer()
        load(e)
    }

    fun load(e: ActionEvent) {
        root = FXMLLoader.load(javaClass.getResource("/Game3.fxml"))
        stage = ((e.source as Node).scene.window) as Stage
        scene = Scene(root)
        stage.scene = scene
        stage.show()
    }

    override fun start(primaryStage: Stage?) {
        val root: Parent = FXMLLoader.load(javaClass.getResource("/Menu3.fxml"))
        val scene = Scene(root)
        primaryStage?.title = "Chess Engine"
        primaryStage?.isResizable = false
        primaryStage?.scene = scene
        primaryStage?.show()
    }

//    override fun viewBoard(board: ChessBoard) {
//        // Do nothing
//    }
}