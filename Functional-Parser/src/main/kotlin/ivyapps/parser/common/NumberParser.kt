package ivyapps.parser.common

import ivyapps.parser.*

fun digit(): Parser<Char> = _sat { it.isDigit() }

/**
 * Parses an integer number without a sign.
 */
fun _int(): Parser<Int> = _oneOrMany(digit()).flatMap { digits ->
    val number = try {
        digits.joinToString(separator = "").toInt()
    } catch (e: Exception) {
        Int.MAX_VALUE
    }
    pure(number)
}

/**
 * Parses a decimal number from as a string as double.
 *
 * **Supported formats:**
 * - 3.14, 1024.0 _"#.#"_
 * - .5, .9 _".#"_
 * - "3." 15. _"#."_
 * - 3, 5, 8 _"#"_
 */
fun _number(): Parser<Double> {
    fun oneOrMoreDigits(): Parser<String> = _oneOrMany(digit()).flatMap { digits ->
        pure(digits.joinToString(separator = ""))
    }

    return _int().flatMap { intPart ->
        // 3.14, ###.00
        _char('.').flatMap {
            oneOrMoreDigits().flatMap { decimalPart ->
                pure("$intPart.$decimalPart".toDouble())
            }
        }
    } _or _char('.').flatMap {
        // .5 => 0.5
        oneOrMoreDigits().flatMap { decimalPart ->
            pure("0.$decimalPart".toDouble())
        }
    } _or _int().flatMap { intPart ->
        // 3. => 3.0
        _char('.').flatMap {
            pure(intPart.toDouble())
        }
    } _or _int().flatMap {
        // 3, 5, 13
        pure(it.toDouble())
    }
}