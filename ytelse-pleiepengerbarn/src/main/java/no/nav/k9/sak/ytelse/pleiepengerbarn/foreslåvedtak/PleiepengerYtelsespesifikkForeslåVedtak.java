package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Optional;

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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class PleiepengerYtelsespesifikkForeslåVedtak implements YtelsespesifikkForeslåVedtak {

    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagTjeneste sykdomGrunnlagTjeneste;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private AksjonspunktRepository aksjonspunktRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    @Inject
    public PleiepengerYtelsespesifikkForeslåVedtak(SamtidigUttakTjeneste samtidigUttakTjeneste,
                                                   BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                                   BehandlingRepository behandlingRepository,
                                                   SykdomGrunnlagTjeneste sykdomGrunnlagTjeneste,
                                                   AksjonspunktKontrollRepository aksjonspunktKontrollRepository,
                                                   AksjonspunktRepository aksjonspunktRepository,
                                                   SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagTjeneste = sykdomGrunnlagTjeneste;
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }

    @Override
    public BehandleStegResultat run(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        if (samtidigUttakTjeneste.isSkalHaTilbakehopp(ref)) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
            return BehandleStegResultat.tilbakeførtTilStegUtenVidereKjøring(BehandlingStegType.VURDER_UTTAK_V2);
        }

        if (søknadsperiodeTjeneste.utledFullstendigPeriode(ref.getBehandlingId()).isEmpty()) {
            return null;
        }
        var lås = behandlingRepository.taSkriveLås(behandling);

        MedisinskGrunnlag medisinskGrunnlag = sykdomGrunnlagTjeneste.hentGrunnlag(behandling.getUuid());
        boolean harUbesluttedeSykdomsVurderinger = medisinskGrunnlag.getGrunnlagsdata().getVurderinger()
            .stream()
            .anyMatch(v -> !v.isBesluttet());

        Optional<Aksjonspunkt> sykdomAP = behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE);
        if (harUbesluttedeSykdomsVurderinger) {
            if (sykdomAP.isEmpty()) {
                Aksjonspunkt aksjonspunkt = aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
                aksjonspunktKontrollRepository.setTilUtført(aksjonspunkt, "Automatisk gjenbruk av ubesluttede vurderinger fra annen fagsak.");
                behandlingRepository.lagre(behandling, lås);
            } else if (sykdomAP.map(Aksjonspunkt::isToTrinnsBehandling).map(it -> !it).orElse(false)) {
                Aksjonspunkt aksjonspunkt = sykdomAP.get();
                aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
                behandlingRepository.lagre(behandling, lås);
            }
        } else if (sykdomAP.map(Aksjonspunkt::isToTrinnsBehandling).orElse(false)) {
            aksjonspunktRepository.fjernToTrinnsBehandlingKreves(sykdomAP.get());
            behandlingRepository.lagre(behandling, lås);
        }

        return null;
    }
}
