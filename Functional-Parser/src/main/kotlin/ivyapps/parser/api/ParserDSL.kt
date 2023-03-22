package ivyapps.parser.api

interface Something<T> {

}

interface ParseScope {
    fun item(): Something<Char>
    fun char(c: Char): Something<Char>
    fun string(str: String): Something<String>
    fun sats(predicate: (Char) -> Boolean): Something<Char>

    infix fun <T> Something<T>.or(other: Something<T>): Something<T>

    fun <T> zeroOrMany(parser: Something<T>): Something<List<T>>
    fun <T> oneOrMany(parser: Something<T>): Something<List<T>>


    suspend fun <T> Something<T>.bind(): T
    suspend fun <T> bind(parser: Something<T>): T
    suspend fun <T> Something<T>.bindOptional(): T?
    suspend fun <T> bindOptional(parser: Something<T>): T?
    suspend fun failure()

    // region Helpers
    fun letter(): Something<Char>
    fun digit(): Something<Int>
    fun integer(): Something<Int>
    fun double(): Something<Double>
    // endregion
}

fun <A, B> takeLeft(a: A, b: B): A = a
fun <A, B> takeRight(a: A, b: B): B = b

fun <T> parse(input: String, parser: suspend ParseScope.() -> T): T? {
    TODO()
}

fun <T> parser(parse: suspend ParseScope.() -> T): Something<T> {
    TODO()
}


fun demo1() {

    val result = parse("Jetpack Compose") {
        val words = zeroOrMany(
            parser {
                val word = oneOrMany(letter()).bind().toString()
                if (word == "bad word") failure()
                char(' ').bindOptional()
                word
            } or parser {
                bind(
                    parser {
                        val number = integer().bind()
                        string("*?").bindOptional()
                        (number * 3).toString()
                    } or parser {
                        val alphaNums = oneOrMany(sats { it.isLetter() || it.isDigit() }).bind()
                        alphaNums.toString()
                    }
                )
                bind(string("okay") or string("no"))
            }
        ).bind()
    }
}