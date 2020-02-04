import scala.util.matching.Regex

val pattern: Regex = """(?=>; rel="next")(?:.*)(?<=page=)(.*)(?=>; rel="last")""".r
val abc = """(?<=page=)(.*)(?=>; rel="last")""".r
val string = """<https://api.github.com/organizations/9892522/repos?page=5>; rel="last""""

val stringg = """<https://api.github.com/organizations/9892522/repos?page=2>; rel="next", <https://api.github.com/organizations/9892522/repos?page=5>; rel="last""""

val hahah = """<https://api.github.com/organizations/9892522/repos?page=1>; rel="prev", <https://api.github.com/organizations/9892522/repos?page=3>; rel="next", <https://api.github.com/organizations/9892522/repos?page=5>; rel="last", <https://api.github.com/organizations/9892522/repos?page=1>; rel="first""""

val pages: Option[Regex.Match] = abc findFirstMatchIn  stringg

val pag = pages.getOrElse("not fund").toString

val xxx = pattern findAllIn hahah group(1)