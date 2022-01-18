package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
class PSBTilBrevkode implements MapTilBrevkode {

    @Override
    public Brevkode getBrevkode() {
        return Brevkode.PLEIEPENGER_BARN_SOKNAD;
    }
}
