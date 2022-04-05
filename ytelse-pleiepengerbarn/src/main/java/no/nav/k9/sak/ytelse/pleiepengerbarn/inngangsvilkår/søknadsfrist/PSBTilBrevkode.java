package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
class PSBTilBrevkode implements MapTilBrevkode {

    @Override
    public Brevkode getBrevkode() {
        return Brevkode.PLEIEPENGER_BARN_SOKNAD;
    }
}
