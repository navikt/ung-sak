package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.RisikoklassifiseringEventPubliserer;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikoklassifiseringEvent;


@BehandlingStegRef(kode = "INREG")
@BehandlingTypeRef
@FagsakYtelseTypeRef("*")
@ApplicationScoped
public class InnhentRegisteropplysningerStegImpl implements InnhentRegisteropplysningerSteg {

    private BehandlingRepository behandlingRepository;
    private RisikoklassifiseringEventPubliserer eventPubliserer;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    InnhentRegisteropplysningerStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentRegisteropplysningerStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                               BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                               RisikoklassifiseringEventPubliserer eventPubliserer) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.eventPubliserer = eventPubliserer;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        behandlingProsesseringTjeneste.opprettTasksForInitiellRegisterInnhenting(behandling);

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            RisikoklassifiseringEvent risikoklassifiseringEvent = new RisikoklassifiseringEvent(behandling);
            eventPubliserer.fireEvent(risikoklassifiseringEvent);
        }

        return BehandleStegResultat.settPåVent();
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
