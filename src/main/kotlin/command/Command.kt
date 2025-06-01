package command

/**
 * The rubric for communication with the game controller
 */
interface Command {
    /**
     * The action that a command does on the model.
     */
    fun exec()
}