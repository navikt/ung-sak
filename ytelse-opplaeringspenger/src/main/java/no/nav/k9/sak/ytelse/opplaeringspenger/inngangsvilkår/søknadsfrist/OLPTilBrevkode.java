package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.søknadsfrist;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.MapTilBrevkode;

@FagsakYtelseTypeRef("OLP")
@ApplicationScoped
class OLPTilBrevkode implements MapTilBrevkode {

    @Override
    public Brevkode getBrevkode() {
        return Brevkode.OPPLÆRINGSPENGER_SOKNAD;
    }
}
