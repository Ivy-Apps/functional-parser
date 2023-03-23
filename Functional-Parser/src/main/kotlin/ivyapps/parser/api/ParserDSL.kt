package ivyapps.parser.api

import ivyapps.parser.*
import ivyapps.parser.common._int
import ivyapps.parser.common._letter
import ivyapps.parser.common._number

object ParseError : RuntimeException()

class ParseScopeImpl(text: String) : ParseScope {
    private var _leftover: String = text
    override val leftover: String
        get() = _leftover

    override fun item(): Parser<Char> = _item()
    override fun char(c: Char): Parser<Char> = _char(c)
    override fun string(str: String): Parser<String> = _string(str)
    override fun sats(predicate: (Char) -> Boolean): Parser<Char> = _sat(predicate)

    override fun <T> (Parser<T>).or(other: Parser<T>): Parser<T> = _or(other)
    override fun <T> zeroOrMany(parser: Parser<T>): Parser<List<T>> = _zeroOrMany(parser)
    override fun <T> oneOrMany(parser: Parser<T>): Parser<List<T>> = _oneOrMany(parser)

    override suspend fun <T> run(parser: Parser<T>): T = parser.bind()
    override suspend fun <T> runOptional(parser: Parser<T>): T? = parser.bindOptional()
    override suspend fun <T> Parser<T>.bind(): T = bindOptional() ?: failure()
    override suspend fun <T> (Parser<T>).bindOptional(): T? {
        val results = this(leftover)
        return when (results.isEmpty()) {
            true -> null
            false -> {
                val first = results.first()
                _leftover = first.leftover
                first.value
            }
        }
    }

    override suspend fun failure() = throw ParseError
    override suspend fun <T> parser(p: suspend ParseScope.() -> T): Parser<T> = { text ->
        val res = parse(text, p)
        if (res != null) listOf(res) else emptyList()
    }

    override fun letter(): Parser<Char> = _letter()
    override fun digit(): Parser<Int> = _item().flatMap {
        if (it.isDigit()) pure(it.digitToInt()) else fail()
    }

    override fun integer(): Parser<Int> = _int()
    override fun double(): Parser<Double> = _number()
}

interface ParseScope {
    val leftover: String

    fun item(): Parser<Char>
    fun char(c: Char): Parser<Char>
    fun string(str: String): Parser<String>
    fun sats(predicate: (Char) -> Boolean): Parser<Char>

    infix fun <T> Parser<T>.or(other: Parser<T>): Parser<T>
    fun <T> zeroOrMany(parser: Parser<T>): Parser<List<T>>
    fun <T> oneOrMany(parser: Parser<T>): Parser<List<T>>

    suspend fun <T> Parser<T>.bind(): T
    suspend fun <T> run(parser: Parser<T>): T
    suspend fun <T> Parser<T>.bindOptional(): T?
    suspend fun <T> runOptional(parser: Parser<T>): T?
    suspend fun failure()
    suspend fun <T> parser(p: suspend ParseScope.() -> T): Parser<T>

    fun letter(): Parser<Char>
    fun digit(): Parser<Int>
    fun integer(): Parser<Int>
    fun double(): Parser<Double>
}

suspend fun <T> parse(text: String, parser: suspend ParseScope.() -> T): ParseResult<T>? = try {
    val scope = ParseScopeImpl(text)
    val res = with(scope) {
        parser()
    }
    ParseResult(value = res, leftover = scope.leftover)
} catch (e: Exception) {
    null
}

suspend fun main() {
    val res = parse("SET \"this is fake\" 1234", ParseScope::setCommandParser)

    println(res)
    if (res != null) {
        println("SET \"${res.value.key}\" \"${res.value.value}\"")
    }
}

data class Set(val key: String, val value: String)

private suspend fun ParseScope.setCommandParser(): Set {
    string("SET").bind()
    char(' ').bind()
    val key = argumentParser().bind()
    char(' ').bind()
    val value = argumentParser().bind()
    return Set(key, value)
}

private suspend fun ParseScope.argumentParser(): Parser<String> = parser {
    char('"').bind()
    val key = oneOrMany(sats { it != '"' }).bind()
    char('"').bind()
    key.joinToString(separator = "")
} or parser {
    oneOrMany(sats { it != ' ' }).bind().joinToString(separator = "")
}