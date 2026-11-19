package splendor.presentation.console

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ConsoleInput(
    input: InputStream = System.`in`
) {
    private val reader = BufferedReader(InputStreamReader(input))

    fun readText(): String? {
        return reader.readLine()?.trim()
    }

    fun readInteger(): Int? {
        return readText()?.toIntOrNull()
    }

    fun readChoice(): Int? {
        return readInteger()
    }
}
