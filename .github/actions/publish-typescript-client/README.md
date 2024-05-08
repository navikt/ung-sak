actions/publish-typescript-client
==================================

Dette er ein custom composite action som publiserer typescript klient bygd av "generate-typescript-client" action til 
GitHub npm registry

Det er tenkt at denne skal brukast i jobb som laster ned typescript klient kode bygd i tidlegare jobb frå artifact storage.

Det er og tenkt at den berre køyrast ved merge til master, eller på anna vis er begrensa til å køyre når grunnlagskoden
blir deploya til dev/prod.

### Inputs

- _githubToken_: secrets.GITHUB_TOKEN frå kallande workflow. Må ha lov til å publisere npm pakke.

