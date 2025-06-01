import command.Command
import view.GuiMenu
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class Runner {
    private constructor()

    companion object {
        private var running = false
        private val queue: BlockingQueue<Command> = LinkedBlockingQueue()

        // this is dummy code, Ideally I'd like to have a structure which is well suited for tasks
        fun startUp() {
            running = true
            Thread {
                javafx.application.Application.launch(view.GuiMenu::class.java)
            }.start()
            // or run text version

            while (running) {
                val command = queue.take()
                command.exec()
            }
        }

        fun receiveCommand(command: Command) {
            queue.put(command)
        }

    }
}

fun main() {
    Runner.startUp()
}