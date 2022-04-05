package no.nav.k9.sak.ytelse.omsorgspenger.kompletthetssjekk;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.POSTCONDITION_KOMPLETTHET;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
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
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(value = POSTCONDITION_KOMPLETTHET)
@BehandlingTypeRef
@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class PostconditionKompletthetSteg implements BehandlingSteg {

    private OmsorgspengerKompletthetsjekker kompletthetsjekker;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    PostconditionKompletthetSteg() {
    }

    @Inject
    public PostconditionKompletthetSteg(@FagsakYtelseTypeRef(OMSORGSPENGER) OmsorgspengerKompletthetsjekker kompletthetsjekker,
                                        BehandlingRepositoryProvider provider,
                                        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                        HenleggBehandlingTjeneste henleggBehandlingTjeneste) {
        this.kompletthetsjekker = kompletthetsjekker;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erManueltOpprettet() && behandling.erRevurdering()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        if (kompletthetsjekker.ingenSøknadsperioder(ref)) {
            henleggBehandlingTjeneste.lagHistorikkInnslagForHenleggelseFraSteg(behandling.getId(), BehandlingResultatType.HENLAGT_MASKINELT, "Kan ikke fortsette uten søknadsperioder");
            return BehandleStegResultat.henlagtBehandling();
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
