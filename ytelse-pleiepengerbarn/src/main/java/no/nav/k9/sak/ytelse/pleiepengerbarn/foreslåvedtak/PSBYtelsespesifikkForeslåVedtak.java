package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

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
@FagsakYtelseTypeRef("PSB")
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

        if (harUbesluttedeSykdomsVurderinger) {
            Optional<Aksjonspunkt> ap9001 = behandling.getAksjonspunkter().stream().filter(a -> a.getAksjonspunktDefinisjon().getKode().equals(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE)).findFirst();
            Aksjonspunkt aksjonspunkt;
            if (ap9001.isEmpty()) {
                aksjonspunkt = aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
            } else {
                aksjonspunkt = ap9001.get();
            }
            if (aksjonspunkt.getStatus().erÅpentAksjonspunkt()) {
                aksjonspunktKontrollRepository.setTilUtført(aksjonspunkt, "Maskinelt utført - Vurderinger besluttet på annen part");
                behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
            }
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return null;
    }

}
