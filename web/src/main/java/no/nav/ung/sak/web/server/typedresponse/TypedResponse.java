package no.nav.ung.sak.web.server.typedresponse;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;

/**
 * Brukes til å markere responstype frå rest endepunkt metode som lager en standardrespons med en spesifikk type, men
 * også blir prosessert av TypedResponseFilter. Dette gir mulighet for å overstyre standard respons ved å returnere
 * spesifikke tilleggstyper til dette interface. Det at TypedResponse er definert med @NotNull @JsonValue gjer at
 * generert openapi spesifikasjon får gitt type som default respons type slik vi ønsker.
 * <p>
 *     Standard respons returnerast ved å returnere ein instans av {@link EntityResponse}. På den kan ein også legge til
 *     ekstra metadata som f.eks ETag, som så blir lagt til respons headers av TypedResponseFilter.
 * </p>
 * <p>
 *     Viss ein skal returnere spesielle tomme responser kan ein returnere ein instans av {@link SpecialEmptyResponse}.
 *     Denne brukast for eksempel viss ein skal returnere 304 Not Modified som svar på request med matchande etag.
 * </p>
 * <p>
 *     {@link TypedResponseFilter} for meir informasjon om kva overstyringer som kan gjerast ved å returnere ulike
 *     TypedResponse implementasjoner. Ein kan og utvide TypedResponseFilter for å legge til meir slik funksjonalitet.
 *     Ein bør passe på at evt endringer TypedResponseFilter gjere ikkje fører til at openapi spesifikasjonen blir missvisande.
 * </p>
 * <p>
 *     Sjå eksisterande bruk av TypedResponse i koden for meir informasjon om korleis det kan brukast.
 * </p>
 *
 * @param <ENTITY> Typen av enheten som returneres som standardrespons når ingen andre spesielle omstendigheter gjelder.
 */
public interface TypedResponse<ENTITY> {
    @NotNull
    @JsonValue
    public ENTITY getEntity();
}
