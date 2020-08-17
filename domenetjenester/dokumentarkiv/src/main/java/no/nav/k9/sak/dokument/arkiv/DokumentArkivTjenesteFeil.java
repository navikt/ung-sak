package no.nav.k9.sak.dokument.arkiv;


import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.IntegrasjonFeil;
import no.nav.vedtak.felles.integrasjon.saf.graphql.HentDokumentQuery;

public interface DokumentArkivTjenesteFeil extends DeklarerteFeil {

    DokumentArkivTjenesteFeil FACTORY = FeilFactory.create(DokumentArkivTjenesteFeil.class);

    @IntegrasjonFeil(feilkode = "FP-249791", feilmelding = "Fant ikke journal dokument: %s", logLevel = LogLevel.WARN)
    Feil hentDokumentIkkeFunnet(HentDokumentQuery query);

}
