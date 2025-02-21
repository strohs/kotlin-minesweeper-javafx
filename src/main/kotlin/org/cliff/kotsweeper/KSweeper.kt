package org.cliff.kotsweeper

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.MenuBar
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage


/**
 * This is the main JavaFX gui launching point as well as the main Controller for the various GUI elements. Other
 * controllers will call functions on this controller to interact with the game's model
 * User: Cliff
 * Time: 4:05 PM
 */
class KSweeper : Application() {

    companion object {

        const val BORDER_ID           = "borderPane"
        const val MAIN_GRID_ID        = "main-grid"
        const val SMILEY_BUTTON_ID    = "smiley-button"
        const val MENU_BAR_ID         = "mainMenuBar"
        const val VBOX_ID             = "centerVBox"
        const val ROW_SLIDER_ID       = "rowSlider"
        const val COL_SLIDER_ID       = "colSlider"
        const val REMAINING_MINES_ID  = "remainingMines"
        const val TIMER_ID            = "timer"
        const val IMAGES_PATH         = "graphics"
        const val STYLESHEET_PATH     = "stylesheets/kotsweeper.css"

        const val SMILEY_TT_TEXT      = "Click to start a new game"
        const val REMAINING_TT_TEXT   = "Number of mines remaining"

        const val BLACK_FLAG = "\u2691"
        const val BOMB = "\u2299"
        const val QUESTION_MARK = "?"
        //val SMILEY_FACE = "\u263A"
        const val MISMARKED = "X"

        /**
         * javafx launching point
         */
        @JvmStatic fun main(args: Array<String>) {
            launch( KSweeper::class.java, *args )
        }
    }

    //model that holds the state of the board
    val model = Model( 8,8, BoardModel.newBoard( 8,8 ) )

    //references to all controllers used in the game
    private lateinit var timerCtrl: TimerCtrl
    private lateinit var smileyBtnCtrl: SmileyBtnCtrl
    private lateinit var remainingMinesCtrl: RemainingMinesCtrl
    private lateinit var optionsMenuCtrl: OptionsMenuCtrl
    private lateinit var gridCtrl: GridCtrl
    //holds a reference to our javafx Scene
    private lateinit var scene:Scene


    override fun start( stage: Stage ) {
        // Use a border pane as the root for our scene
        val borderPane = BorderPane()
        borderPane.id = BORDER_ID

        //border pane will have a menu bar across the top
        borderPane.top = buildMenus()

        val hbox = addHBox()
        gridCtrl = GridCtrl( this,  model.rows, model.cols )

        //center of the border pane will have a VBOX that contains the main grid and some score counters
        val vbox = buildVbox()
        vbox.children.addAll( hbox, gridCtrl.grid )
        borderPane.center = vbox


        val scene = Scene( borderPane )
        scene.stylesheets.add( STYLESHEET_PATH )
        stage.scene = scene
        stage.title = "KotSweeper"
        BoardPrinter.print( model.boardState, true )
        this.scene = scene

        stage.show()
    }


    private fun buildVbox() : VBox {
        val vbox = VBox()
        vbox.id = VBOX_ID
        return vbox
    }


    /**
     * build the HBox that holds the elapsedTime, smileyBtn and remainingMines controls
     */
    private fun addHBox(): HBox {
        val hbox = HBox()
        hbox.styleClass.add("top-hbox")

        remainingMinesCtrl = RemainingMinesCtrl( this )
        smileyBtnCtrl = SmileyBtnCtrl( this )
        timerCtrl = TimerCtrl( this )
        hbox.children.addAll( remainingMinesCtrl.textField, smileyBtnCtrl.btn, timerCtrl.timer )
        
        return hbox
    }

    /**
     * builds the games MenuBar and submenus
     */
    private fun buildMenus(): MenuBar {
        //create the menu bar
        val menuBar: MenuBar = MenuBar()
        menuBar.id = KSweeper.MENU_BAR_ID
        //create the options menu and its submenus
        optionsMenuCtrl = OptionsMenuCtrl( this )
        menuBar.menus.add( optionsMenuCtrl.optionMenu )

        return menuBar
    }


    /**
     * Resets the game. A new mine grid is generated that takes its data from model.boardState.
     */
    fun resetGame() {
        smileyBtnCtrl.setImage( "$IMAGES_PATH/happySmiley.png" )
        //create a new board state
        model.boardState = BoardModel.newBoard( model.rows, model.cols )
        val vbox = scene.lookup("#$VBOX_ID") as VBox
        //reset the remaining mines text field
        remainingMinesCtrl.setText( BoardModel.unmarkedMineCount( model.boardState ).toString() )
        //remove the old gridPane
        vbox.children.remove( gridCtrl.grid )
        //build a new GridPane and add it back to VBOX
        this.gridCtrl = GridCtrl( this, model.rows, model.cols )
        vbox.children.add( gridCtrl.grid )
        //reset the timer TextField
        timerCtrl.resetTimer()
        //redisplay the game grid
        gridCtrl.refreshGrid( model.boardState )
    }

    /**
     * A game is in the finished state when all the mine markers have been placed OR the user clicks on a mined square.
     * This method will color any squares that were correctly marked, stop the elapsed timer, and also
     * reveal all the squares on the board
     */
    private fun doGameFinished() {
        gridCtrl.colorizeSquares( BoardModel.correctlyMarkedIndices( model.boardState ), Color.GREEN )
        model.boardState = BoardModel.revealBoard( model.boardState )
        timerCtrl.stopTimer()
        //disable event handling on the main grid until a new game is started
        gridCtrl.disableMouseClickEvents()
    }

    /**
     * This is the main handler for left/right clicks on the squares of the Grid.
     * @param clickType the mouse button that was clicked
     * @param sqCtrl the square controller
     */
    fun processGridClick( clickType:MouseButton, sqCtrl: SquareCtrl )  {
        val row = sqCtrl.row
        val col = sqCtrl.col
        when ( clickType ) {

            MouseButton.PRIMARY -> {
                //reveal squares on the game board and update the boardState
                model.boardState = BoardModel.reveal( row,col, model.boardState )
                remainingMinesCtrl.setText( BoardModel.unmarkedMineCount( model.boardState ).toString() )

                //check if mined square was clicked
                if ( model.boardState[row][col].type == SquareType.MINE ) {
                    sqCtrl.rect.fill = Color.RED
                    smileyBtnCtrl.setImage( "${KSweeper.IMAGES_PATH}/sadSmiley.png")
                    doGameFinished()
                }
            }

            MouseButton.SECONDARY -> {
                model.boardState = BoardModel.toggleMark( row, col, model.boardState )
                val remainingMarks = BoardModel.unmarkedMineCount( model.boardState )
                remainingMinesCtrl.setText( BoardModel.unmarkedMineCount( model.boardState ).toString() )

                //check if player has won
                if ( remainingMarks == 0 ) {
                    if ( BoardModel.checkForWin( model.boardState ) ) {
                        smileyBtnCtrl.setImage( "${KSweeper.IMAGES_PATH}/shadesSmiley.png")
                        doGameFinished()
                    }
                    else {
                        //player did mark all mines correctly
                        smileyBtnCtrl.setImage( "${KSweeper.IMAGES_PATH}/sadSmiley.png")
                        doGameFinished()
                    }

                } else {
                    //set the clicked square to a marked flag icon
                    sqCtrl.setSquareText( model.boardState[row][col] )
                }

            }
            else -> {
                println("unsupported mouse click detected: $clickType")
            }
        }
        gridCtrl.refreshGrid( model.boardState )
    }


}