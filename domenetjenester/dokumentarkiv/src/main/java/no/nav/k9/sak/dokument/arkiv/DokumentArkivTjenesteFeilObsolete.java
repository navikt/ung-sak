package no.nav.k9.sak.dokument.arkiv;


import no.nav.k9.sak.dokument.arkiv.saf.graphql.HentDokumentQuery;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.IntegrasjonFeil;

public interface DokumentArkivTjenesteFeilObsolete extends DeklarerteFeil {

    DokumentArkivTjenesteFeilObsolete FACTORY = FeilFactory.create(DokumentArkivTjenesteFeilObsolete.class);

    @IntegrasjonFeil(feilkode = "FP-249790", feilmelding = "Fant ikke journal dokument: %s", logLevel = LogLevel.WARN)
    Feil hentDokumentIkkeFunnet(HentDokumentQuery query);

}
