package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@BehandlingStegRef(kode = "FORVEDSTEG")
@BehandlingTypeRef("BT-004") //Revurdering
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakRevurderingStegImpl implements ForeslåVedtakSteg {

    private BeregningTjeneste kalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;

    ForeslåVedtakRevurderingStegImpl() {
    }

    @Inject
    ForeslåVedtakRevurderingStegImpl(ForeslåVedtakTjeneste foreslåVedtakTjeneste,
                                     BeregningTjeneste kalkulusTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling revurdering = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Behandling orginalBehandling = getOriginalBehandling(revurdering);

        BehandleStegResultat behandleStegResultat = foreslåVedtakTjeneste.foreslåVedtak(revurdering, kontekst);

        if (!beregningsgrunnlagEksisterer(revurdering) || isBehandlingsresultatAvslått(orginalBehandling)) {
            return behandleStegResultat;
        }

        //Oppretter aksjonspunkt dersom revurdering har mindre beregningsgrunnlag enn orginal
        if (erRevurderingensBeregningsgrunnlagMindreEnnOrginal(orginalBehandling, revurdering)) {
            List<AksjonspunktDefinisjon> aksjonspunkter = behandleStegResultat.getAksjonspunktResultater().stream()
                .map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
            aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
        }
        return behandleStegResultat;
    }

    private boolean isBehandlingsresultatAvslått(Behandling orginalBehandling) {
        return getSistBehandlingUtenIngenEndring(orginalBehandling).getBehandlingResultatType().isBehandlingsresultatAvslått();
    }

    private Behandling getSistBehandlingUtenIngenEndring(Behandling behandling) {
        var beh = behandling;
        while (beh.getBehandlingResultatType().isBehandlingsresultatIkkeEndret()) {
            beh = getOriginalBehandling(beh);
        }
        return beh;
    }

    private Behandling getOriginalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandling()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Revurdering skal alltid ha orginal behandling"));
    }

    private boolean beregningsgrunnlagEksisterer(Behandling behandling) {
        return kalkulusTjeneste.hentFastsatt(behandling.getId()).isPresent();
    }

    private boolean erRevurderingensBeregningsgrunnlagMindreEnnOrginal(Behandling orginalBehandling, Behandling revurdering) {
        Beregningsgrunnlag orginalBeregning = kalkulusTjeneste.hentFastsatt(orginalBehandling.getId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Skal ha Beregningsgrunnlag på orginalbehandling vedtak"));
        Beregningsgrunnlag revurderingsBeregning = kalkulusTjeneste.hentFastsatt(revurdering.getId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Skal ha Beregningsgrunnlag på positivt vedtak"));

        BigDecimal orginalBeregningSumBruttoPrÅr = orginalBeregning.getBeregningsgrunnlagPerioder().stream()
            .map(BeregningsgrunnlagPeriode::getBruttoPrÅr).reduce(new BigDecimal(0), BigDecimal::add);
        BigDecimal revurderingsBeregningSumBruttoPrÅr = revurderingsBeregning.getBeregningsgrunnlagPerioder().stream()
            .map(BeregningsgrunnlagPeriode::getBruttoPrÅr).reduce(new BigDecimal(0), BigDecimal::add);

        return revurderingsBeregningSumBruttoPrÅr.compareTo(orginalBeregningSumBruttoPrÅr) < 0;
    }

}
