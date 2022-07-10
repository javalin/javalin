package io.javalin.routing


class MissingBracketsException(segment: String, val path: String) : IllegalArgumentException(
    "This segment '$segment' is missing some brackets! Found in path '$path'"
)

class WildcardBracketAdjacentException(segment: String, val path: String) : IllegalArgumentException(
    "Wildcard and a path parameter bracket are adjacent in segment '$segment' of path '$path'. This is forbidden"
)

class ParameterNamesNotUniqueException(val path: String) : IllegalArgumentException(
    "Duplicate path param names detected! This is forbidden. Found in path '$path'"
)
