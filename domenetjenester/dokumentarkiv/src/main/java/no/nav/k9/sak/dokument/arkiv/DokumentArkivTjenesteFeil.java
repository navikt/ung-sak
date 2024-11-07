package no.nav.k9.sak.dokument.arkiv;


import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;
import no.nav.k9.felles.integrasjon.saf.HentDokumentQuery;

public interface DokumentArkivTjenesteFeil extends DeklarerteFeil {

    DokumentArkivTjenesteFeil FACTORY = FeilFactory.create(DokumentArkivTjenesteFeil.class);

    @IntegrasjonFeil(feilkode = "FP-249791", feilmelding = "Fant ikke journal dokument: %s", logLevel = LogLevel.WARN)
    Feil hentDokumentIkkeFunnet(HentDokumentQuery query);

}
