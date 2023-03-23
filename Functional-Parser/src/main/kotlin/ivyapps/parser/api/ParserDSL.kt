package ivyapps.parser.api

import ivyapps.parser.*
import ivyapps.parser.common.number
import ivyapps.parser.or as _or

object ParseError : RuntimeException()

class ParseScopeImpl(text: String) : ParseScope {
    private var _leftover: String = text
    override val leftover: String
        get() = _leftover

    override fun item(): Parser<Char> = ivyapps.parser.item()
    override fun sats(predicate: (Char) -> Boolean): Parser<Char> = sat(predicate)
    override fun char(c: Char): Parser<Char> = ivyapps.parser.char(c)

    override fun string(str: String): Parser<String> = ivyapps.parser.string(str)
    override fun takeWhile(predicate: (Char) -> Boolean): Parser<String> =
        zeroOrMany(sat(predicate)).flatMap {
            pure(it.joinToString(separator = ""))
        }

    override fun <T> (Parser<T>).or(other: Parser<T>): Parser<T> = _or(other)
    override fun <T> zeroOrMany(parser: Parser<T>): Parser<List<T>> = ivyapps.parser.zeroOrMany(parser)
    override fun <T> oneOrMany(parser: Parser<T>): Parser<List<T>> = ivyapps.parser.oneOrMany(parser)

    override suspend fun <T> Parser<T>.bind(): T = bindOptional() ?: fail()
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

    override suspend fun fail(): Nothing = throw ParseError


    override fun int(): Parser<Int> = ivyapps.parser.common.int()
    override fun double(): Parser<Double> = number()
}

interface ParseScope {
    val leftover: String

    fun item(): Parser<Char>
    fun sats(predicate: (Char) -> Boolean): Parser<Char>
    fun char(c: Char): Parser<Char>

    fun string(str: String): Parser<String>
    fun takeWhile(predicate: (Char) -> Boolean): Parser<String>

    infix fun <T> Parser<T>.or(other: Parser<T>): Parser<T>
    fun <T> zeroOrMany(parser: Parser<T>): Parser<List<T>>
    fun <T> oneOrMany(parser: Parser<T>): Parser<List<T>>

    suspend fun <T> Parser<T>.bind(): T
    suspend fun <T> Parser<T>.bindOptional(): T?
    suspend fun fail(): Nothing

    fun int(): Parser<Int>
    fun double(): Parser<Double>
}


fun <T> parser(p: suspend ParseScope.() -> T): Parser<T> = { text ->
    val res = parse(text, p)
    if (res != null) listOf(res) else emptyList()
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

suspend fun <T> Parser<T>.parse(text: String): ParseResult<T>? = try {
    this(text).firstOrNull()
} catch (e: Exception) {
    null
}