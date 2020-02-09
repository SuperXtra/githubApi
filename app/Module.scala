import com.google.inject.AbstractModule
import io.scalac.config.GithubApiConfig
import pureconfig._
import com.typesafe.config.ConfigFactory


// TODO: rename to GithubApiModule (and make sure that there is right name in the config)
// TODO where in config should I change that?
class Module extends AbstractModule {

  override def configure() = {

    val config = ConfigFactory.load("github-api.conf")
    val githubApiConfig = ConfigSource.fromConfig(config).loadOrThrow[GithubApiConfig]
    bind(classOf[GithubApiConfig]).toInstance(githubApiConfig)

    // TODO: You might register your dependencies here
  }

}

