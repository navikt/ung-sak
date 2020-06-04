package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;

@BehandlingStegRef(kode = "FORVEDSTEG")
@BehandlingTypeRef("BT-004") //Revurdering
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakRevurderingStegImpl implements ForeslåVedtakSteg {

    private BeregningTjeneste kalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    ForeslåVedtakRevurderingStegImpl() {
    }

    @Inject
    ForeslåVedtakRevurderingStegImpl(ForeslåVedtakTjeneste foreslåVedtakTjeneste,
                                     BeregningTjeneste kalkulusTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling revurdering = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Behandling orginalBehandling = getOriginalBehandling(revurdering);
        var revurderingRef = BehandlingReferanse.fra(revurdering);
        var orginalRef = BehandlingReferanse.fra(orginalBehandling);
        BehandleStegResultat behandleStegResultat = foreslåVedtakTjeneste.foreslåVedtak(revurdering, kontekst);

        //Oppretter aksjonspunkt dersom revurdering har mindre beregningsgrunnlag enn orginal
        var skjæringstidspunkter = vilkårResultatRepository.hent(revurdering.getId())
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .collect(Collectors.toList());

        for (LocalDate skjæringstidspunkt : skjæringstidspunkter) {
            if (erRevurderingensBeregningsgrunnlagMindreEnnOrginal(orginalRef, revurderingRef, skjæringstidspunkt)) {
                List<AksjonspunktDefinisjon> aksjonspunkter = behandleStegResultat.getAksjonspunktResultater().stream()
                    .map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
                aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
                return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
            }
        }
        return behandleStegResultat;
    }

    private Behandling getOriginalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandling()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Revurdering skal alltid ha orginal behandling"));
    }

    private boolean erRevurderingensBeregningsgrunnlagMindreEnnOrginal(BehandlingReferanse orginalBehandling, BehandlingReferanse revurdering, LocalDate skjæringstidspuntk) {
        var orginalBeregning = kalkulusTjeneste.hentFastsatt(orginalBehandling, skjæringstidspuntk);
        var revurderingsBeregning = kalkulusTjeneste.hentFastsatt(revurdering, skjæringstidspuntk);

        return ErEndringIBeregning.vurderUgunst(revurderingsBeregning, orginalBeregning);
    }

}
