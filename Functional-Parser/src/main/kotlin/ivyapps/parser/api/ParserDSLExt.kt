package ivyapps.parser.api

import ivyapps.parser.Parser

suspend fun ParseScope.dropWhile(
    inclusive: Boolean = false,
    predicate: (Char) -> Boolean
) {
    takeWhile(predicate).bindOptional()
    if (inclusive) {
        item().bind()
    }
}

fun ParseScope.takeAllLeft(): Parser<String> = takeWhile { true }
