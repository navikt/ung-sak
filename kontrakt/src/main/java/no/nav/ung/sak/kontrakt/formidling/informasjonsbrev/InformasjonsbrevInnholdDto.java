package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;


/**
 * Representerer innholdet i et informasjonsbrev. Bruker JsonSubTypes for polymorfisme basert å {@link no.nav.ung.kodeverk.dokument.DokumentMalType} kodeverdi.
 */
public sealed interface InformasjonsbrevInnholdDto permits GenereltFritekstBrevDto {
}
