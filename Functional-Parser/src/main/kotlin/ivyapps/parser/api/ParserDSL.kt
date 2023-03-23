package ivyapps.parser.api

import ivyapps.parser.*
import ivyapps.parser.common.int
import ivyapps.parser.common.number
import ivyapps.parser.or as _or

object ParseError : RuntimeException()

class ParseScopeImpl(text: String) : ParseScope {
    private var _leftover: String = text
    override val leftover: String
        get() = _leftover

    override fun item(): Parser<Char> = ivyapps.parser.item()
    override fun char(c: Char): Parser<Char> = ivyapps.parser.char(c)
    override fun string(str: String): Parser<String> = ivyapps.parser.string(str)
    override fun sats(predicate: (Char) -> Boolean): Parser<Char> = sat(predicate)

    override fun <T> (Parser<T>).or(other: Parser<T>): Parser<T> = _or(other)
    override fun <T> zeroOrMany(parser: Parser<T>): Parser<List<T>> = ivyapps.parser.zeroOrMany(parser)
    override fun <T> oneOrMany(parser: Parser<T>): Parser<List<T>> = ivyapps.parser.oneOrMany(parser)

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

    override fun letter(): Parser<Char> = ivyapps.parser.common.letter()
    override fun digit(): Parser<Int> = ivyapps.parser.item().flatMap {
        if (it.isDigit()) pure(it.digitToInt()) else fail()
    }

    override fun integer(): Parser<Int> = int()
    override fun double(): Parser<Double> = number()
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
    suspend fun <T> Parser<T>.bindOptional(): T?
    suspend fun failure()

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

fun <T> parser(p: suspend ParseScope.() -> T): Parser<T> = { text ->
    val res = parse(text, p)
    if (res != null) listOf(res) else emptyList()
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

private suspend fun argumentParser(): Parser<String> = parser {
    char('"').bind()
    val key = oneOrMany(sats { it != '"' }).bind()
    char('"').bind()
    key.joinToString(separator = "")
} _or parser {
    oneOrMany(sats { it != ' ' }).bind().joinToString(separator = "")
}