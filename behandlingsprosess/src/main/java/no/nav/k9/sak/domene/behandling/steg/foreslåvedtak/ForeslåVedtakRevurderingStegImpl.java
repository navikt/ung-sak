package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
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
import no.nav.k9.sak.dokument.bestill.tjenester.FormidlingDokumentdataTjeneste;

@BehandlingStegRef(kode = "FORVEDSTEG")
@BehandlingTypeRef("BT-004") //Revurdering
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakRevurderingStegImpl implements ForeslåVedtakSteg {

    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<ErEndringIBeregningVurderer> endringIBeregningTjenester;
    private FormidlingDokumentdataTjeneste formidlingDokumentdataTjeneste;

    ForeslåVedtakRevurderingStegImpl() {
    }

    @Inject
    ForeslåVedtakRevurderingStegImpl(ForeslåVedtakTjeneste foreslåVedtakTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider,
                                     @Any Instance<ErEndringIBeregningVurderer> endringIBeregningTjenester,
                                     FormidlingDokumentdataTjeneste formidlingDokumentdataTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
        this.endringIBeregningTjenester = endringIBeregningTjenester;
        this.formidlingDokumentdataTjeneste = formidlingDokumentdataTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling revurdering = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Behandling orginalBehandling = getOriginalBehandling(revurdering);
        var revurderingRef = BehandlingReferanse.fra(revurdering);
        var originalRef = BehandlingReferanse.fra(orginalBehandling);
        BehandleStegResultat behandleStegResultat = foreslåVedtakTjeneste.foreslåVedtak(revurdering, kontekst);

        //Oppretter aksjonspunkt dersom revurdering har mindre beregningsgrunnlag enn orginal
        NavigableSet<LocalDate> skjæringstidspunkter = vilkårResultatRepository.hent(revurdering.getId())
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .collect(Collectors.toCollection(TreeSet::new));

        var vurderUgunst = erRevurderingensBeregningsgrunnlagMindreEnnOrginal(originalRef, revurderingRef, skjæringstidspunkter);
        for (LocalDate stp : skjæringstidspunkter) {
            if (vurderUgunst.containsKey(stp) && vurderUgunst.get(stp)) {
                // stopp på første som har aksjonspunkt
                List<AksjonspunktDefinisjon> aksjonspunkter = behandleStegResultat.getAksjonspunktResultater().stream()
                    .map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
                aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
                return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
            }
        }
        return behandleStegResultat;
    }

    private Behandling getOriginalBehandling(Behandling behandling) {
        var originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Revurdering skal alltid ha orginal behandling"));
        return behandlingRepository.hentBehandling(originalBehandlingId);
    }

    private Map<LocalDate, Boolean> erRevurderingensBeregningsgrunnlagMindreEnnOrginal(BehandlingReferanse orginalBehandling, BehandlingReferanse revurdering,
                                                                                       NavigableSet<LocalDate> skjæringstidspuntk) {
        var endringIBeregningTjeneste = FagsakYtelseTypeRef.Lookup.find(endringIBeregningTjenester, revurdering.getFagsakYtelseType())
            .orElseThrow();

        return endringIBeregningTjeneste.vurderUgunst(orginalBehandling, revurdering, skjæringstidspuntk);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.FORESLÅ_VEDTAK.equals(tilSteg)) {
            formidlingDokumentdataTjeneste.ryddVedTilbakehopp(kontekst.getBehandlingId());
        }
    }
}
