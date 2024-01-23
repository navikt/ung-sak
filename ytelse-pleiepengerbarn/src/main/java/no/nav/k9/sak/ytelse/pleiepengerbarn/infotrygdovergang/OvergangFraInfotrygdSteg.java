package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.OVERGANG_FRA_INFOTRYGD;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(value = OVERGANG_FRA_INFOTRYGD)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@BehandlingTypeRef
@ApplicationScoped
public class OvergangFraInfotrygdSteg implements BehandlingSteg {


    private BehandlingRepository behandlingRepository;
    private InfotrygdMigreringTjeneste infotrygdMigreringTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;


    OvergangFraInfotrygdSteg() {
        // for CDI proxy
    }

    @Inject
    public OvergangFraInfotrygdSteg(BehandlingRepository behandlingRepository,
                                    InfotrygdMigreringTjeneste infotrygdMigreringTjeneste,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.infotrygdMigreringTjeneste = infotrygdMigreringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }


    /**
     * Markerer infotrygdperioder
     *
     * @param kontekst
     */
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        infotrygdMigreringTjeneste.finnOgOpprettMigrertePerioder(kontekst.getBehandlingId(), kontekst.getAktørId(), kontekst.getFagsakId());
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }




}
