package no.nav.k9.sak.ytelse.opplaeringspenger.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.VurderBrevTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
public class OLPForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private VurderBrevTjeneste vurderBrevTjeneste;

    OLPForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public OLPForeslåVedtakManueltUtleder(VurderBrevTjeneste vurderBrevTjeneste) {
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

