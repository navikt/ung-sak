package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.søknadsfrist;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.MapTilBrevkode;

@FagsakYtelseTypeRef("PPN")
@ApplicationScoped
class PLSTilBrevkode implements MapTilBrevkode {

    @Override
    public Brevkode getBrevkode() {
        return Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE;
    }
}
