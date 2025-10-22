import command.Command
import model.movement.MagicGenerator
import view.GuiMenu
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

object Runner {
    private var running = false
    private val queue: BlockingQueue<Command> = LinkedBlockingQueue()

    // this is dummy code, Ideally I'd like to have a structure which is well suited for tasks
    fun startUp() {
        running = true
        MagicGenerator.genMagics()
        Thread {
            javafx.application.Application.launch(GuiMenu::class.java)
        }.start()
        // or run text version

        while (running) {
            val command = queue.take()
            try {
                command.exec()
            } catch (e: Exception) {
                println(e.message)
            }

        }
    }

    fun receiveCommand(command: Command) {
        queue.put(command)
    }

}

fun main() {
    Runner.startUp()
}