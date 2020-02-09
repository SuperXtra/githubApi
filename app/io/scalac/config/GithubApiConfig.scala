package io.scalac.config

case class GithubApiConfig(
                            ghToken: String,
                            baseUrl: String,
                            headerRegex: String,
                            cacheTime: Int
                          )
