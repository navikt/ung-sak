package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.YtelsespesifikkForeslåVedtak;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class PSBYtelsespesifikkForeslåVedtak implements YtelsespesifikkForeslåVedtak {

    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private AksjonspunktRepository aksjonspunktRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    @Inject
    public PSBYtelsespesifikkForeslåVedtak(SamtidigUttakTjeneste samtidigUttakTjeneste,
                                           BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                           BehandlingRepository behandlingRepository,
                                           SykdomGrunnlagService sykdomGrunnlagService,
                                           AksjonspunktKontrollRepository aksjonspunktKontrollRepository,
                                           AksjonspunktRepository aksjonspunktRepository,
                                           SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }

    @Override
    public BehandleStegResultat run(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        if (samtidigUttakTjeneste.isSkalHaTilbakehopp(ref)) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
            return BehandleStegResultat.tilbakeførtTilStegUtenVidereKjøring(BehandlingStegType.VURDER_UTTAK);
        }

        if (søknadsperiodeTjeneste.utledFullstendigPeriode(ref.getBehandlingId()).isEmpty()) {
            return null;
        }

        SykdomGrunnlagBehandling sykdomGrunnlagBehandling = sykdomGrunnlagService.hentGrunnlag(behandling.getUuid());
        boolean harUbesluttedeSykdomsVurderinger = sykdomGrunnlagBehandling.getGrunnlag().getVurderinger()
            .stream()
            .anyMatch(v -> !v.isBesluttet());

        if (harUbesluttedeSykdomsVurderinger && behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE).isEmpty()) {
            Aksjonspunkt aksjonspunkt = aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
            aksjonspunktKontrollRepository.setTilUtført(aksjonspunkt, "Automatisk gjenbruk av ubesluttede vurderinger fra annen fagsak.");
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
        }

        if (!harUbesluttedeSykdomsVurderinger && behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE).isPresent()) {
            Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
            if (aksjonspunkt.isToTrinnsBehandling()) {
                aksjonspunktRepository.fjernToTrinnsBehandlingKreves(aksjonspunkt);
                behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            }
        }

        return null;
    }
}
