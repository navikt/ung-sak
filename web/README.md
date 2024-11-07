# web-webapp

web-webapp inneholder blant annet tjenester for hendelser, lese- eller aktivitetstjenester systemet leverer som kan aksesseres av andre systemer i NAV.

Disse leveres som  en av følgende:
* WebServices (WS-\*) med sikring via SAML (tilsv. NAV 3gen virksomhetstjenester)
* REST tjenester med JSON (evt. XML) output.  Sikret vha. OpenID Connect (OIDC) tokens.
* Json feeds.  Disse håndteres likt REST tjenester.  Brukes for å distribuere hendelser.

For tjenester som eksponeres for andre systemer vil de implementere en kontrakt og være dokumenteret i [Tjenestekatalogen](https://confluence.adeo.no/display/SDFS/Tjenestekatalog).


## typescript klient

Rest tjenester beskrives med openapi annotasjoner og kildekode tolkning.

I build pipeline blir det automatisk generert en openapi.json fil som beskriver REST apiet. Ut fra denne blir det også 
autogenerert et typescript klient bibliotek som blir publisert som npm pakke i github registry. Les mer om dette her:

- [k9-sak/README](../README.md)
- [openapi-ts-client/README](src/main/resources/openapi-ts-client/README.md)
