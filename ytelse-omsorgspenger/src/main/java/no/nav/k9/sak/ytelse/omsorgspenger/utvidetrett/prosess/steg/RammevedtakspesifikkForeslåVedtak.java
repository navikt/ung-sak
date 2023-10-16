package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import org.jboss.weld.exceptions.IllegalStateException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.YtelsespesifikkForeslåVedtak;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.PeriodisertUtvidetRettIverksettTjeneste;


@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
public class RammevedtakspesifikkForeslåVedtak implements YtelsespesifikkForeslåVedtak {

    private BehandlingRepository behandlingRepository;

    private PeriodisertUtvidetRettIverksettTjeneste periodisertUtvidetRettIverksettTjeneste;

    public RammevedtakspesifikkForeslåVedtak() {
        //for CDI proxy
    }

    @Inject
    public RammevedtakspesifikkForeslåVedtak(BehandlingRepository behandlingRepository, PeriodisertUtvidetRettIverksettTjeneste periodisertUtvidetRettIverksettTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.periodisertUtvidetRettIverksettTjeneste = periodisertUtvidetRettIverksettTjeneste;
    }

    @Override
    public BehandleStegResultat run(BehandlingReferanse ref) {
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        LocalDateTimeline<Utfall> endringer = periodisertUtvidetRettIverksettTjeneste.utfallSomErEndret(behandling);
        if (endringer.size() > 1) {
            throw new IllegalStateException("Det er ikke støttet i omsorgsdager å motta mer enn 1 periode med endring for hver behandling, stopper derfor her så det kan fikses. Har endringer for perioder: " + endringer);
        }
        return null; //null gir fallthrough til vanlig foreslå-vedtak-implementasjon
    }
}
