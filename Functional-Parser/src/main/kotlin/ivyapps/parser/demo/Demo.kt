package ivyapps.parser.demo

import ivyapps.parser.api.dropWhile
import ivyapps.parser.api.parse
import ivyapps.parser.api.parser


suspend fun main() {
    emailDomainExample()
}

private suspend fun emailDomainExample() {
    val emailDomainParser = parser {
        dropWhile { it != '@' }
        char('@').bind()
        val domain = takeWhile { true }.bind()
        domain
    }

    println(emailDomainParser.parse("iliyan.germanov971@gmail.com"))
}
