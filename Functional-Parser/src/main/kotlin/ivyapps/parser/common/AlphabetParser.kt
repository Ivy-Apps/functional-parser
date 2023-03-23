package ivyapps.parser.common

import ivyapps.parser.Parser
import ivyapps.parser._sat

fun _letter(): Parser<Char> = _sat { it.isLetter() }