# web-webapp

web-webapp inneholder blant annet tjenester for hendelser, lese- eller aktivitetstjenester systemet leverer som kan aksesseres av andre systemer i NAV.

Disse leveres som REST tjenester med JSON output.  Sikret vha. OpenID Connect (OIDC) tokens.


## typescript klient

Rest tjenester beskrives med openapi annotasjoner og kildekode tolkning.

I build pipeline blir det automatisk generert en openapi.json fil som beskriver REST apiet. Ut fra denne blir det også 
autogenerert et typescript klient bibliotek som blir publisert som npm pakke i github registry. Les mer om dette her:

- [k9-sak/README](../README.md)
- [openapi-ts-client/README](src/main/resources/openapi-ts-client/README.md)
