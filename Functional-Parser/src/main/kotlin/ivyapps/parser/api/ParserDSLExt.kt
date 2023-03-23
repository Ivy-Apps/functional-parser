package ivyapps.parser.api

suspend fun ParseScope.dropWhile(predicate: (Char) -> Boolean) {
    takeWhile(predicate).bindOptional()
}
