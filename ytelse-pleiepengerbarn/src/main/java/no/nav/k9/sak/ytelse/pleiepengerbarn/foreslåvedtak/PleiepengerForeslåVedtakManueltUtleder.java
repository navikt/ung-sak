package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.VurderBrevTjeneste;


@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class PleiepengerForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private VurderBrevTjeneste vurderBrevTjeneste;

    PleiepengerForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public PleiepengerForeslåVedtakManueltUtleder(VurderBrevTjeneste vurderBrevTjeneste) {
        this.vurderBrevTjeneste = vurderBrevTjeneste;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return erManuellRevurdering(behandling) || vurderBrevTjeneste.trengerManueltBrev(behandling);
    }

    private boolean erManuellRevurdering(Behandling behandling) {
        return BehandlingType.REVURDERING == behandling.getType() && behandling.erManueltOpprettet();
    }

}
