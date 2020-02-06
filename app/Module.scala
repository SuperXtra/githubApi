import com.google.inject.AbstractModule
import controllers.ApiHelpController
import io.scalac.config.GithubApiConfig
import play.modules.swagger.SwaggerPluginImpl
import pureconfig._
import pureconfig.generic.auto._


// TODO: rename to GithubApiModule (and make sure that there is right name in the config)
class Module extends AbstractModule {

  override def configure() = {

    val githubApiConfig = ConfigSource.defaultApplication.loadOrThrow[GithubApiConfig]

    bind(classOf[GithubApiConfig]).toInstance(githubApiConfig)

    // TODO: You might register your dependencies here
  }

}

