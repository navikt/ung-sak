package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.YtelsespesifikkForeslåVedtak;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class PSBYtelsespesifikkForeslåVedtak implements YtelsespesifikkForeslåVedtak {

    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    @Inject
    public PSBYtelsespesifikkForeslåVedtak(SamtidigUttakTjeneste samtidigUttakTjeneste, BehandlingProsesseringTjeneste behandlingProsesseringTjeneste, BehandlingRepository behandlingRepository, SykdomGrunnlagService sykdomGrunnlagService, AksjonspunktKontrollRepository aksjonspunktKontrollRepository) {
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
    }

    @Override
    public BehandleStegResultat run(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        if (samtidigUttakTjeneste.isSkalHaTilbakehopp(ref)) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
            return BehandleStegResultat.tilbakeførtTilStegUtenVidereKjøring(BehandlingStegType.VURDER_UTTAK);
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

        return null;
    }

}
