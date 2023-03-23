package ivyapps.parser.common

import ivyapps.parser.Parser
import ivyapps.parser.sat

fun letter(): Parser<Char> = sat { it.isLetter() }