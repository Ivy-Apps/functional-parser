package ivyapps.parser.demo

import ivyapps.parser.api.dropWhile
import ivyapps.parser.api.parse
import ivyapps.parser.api.parser
import ivyapps.parser.api.takeAllLeft


suspend fun main() {
    emailDomainExample()
}

private suspend fun emailDomainExample() {
    val emailDomainParser = parser {
        dropWhile(inclusive = true) { it != '@' }
        takeAllLeft().bind()
    }

    println(emailDomainParser.parse("iliyan.germanov971@gmail.com"))
}