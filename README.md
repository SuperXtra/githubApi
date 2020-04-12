# GithubApi

Api that returns ranking of contributors for given organization.

Written in Scala with use of Play framework.

## Environment

`application.conf` - contains technical configuration
`github-api.conf` - contains service configuration

**Important!** service requires an environment variable GH_TOKEN with valid token to github api to run. Example:

```bash
export GH_TOKEN="xxx"
```
env
## Running

```bash
sbt run
```

Listens on port `8080`

## Testing

```bash
sbt test
```

## Deploy

Not implemented
