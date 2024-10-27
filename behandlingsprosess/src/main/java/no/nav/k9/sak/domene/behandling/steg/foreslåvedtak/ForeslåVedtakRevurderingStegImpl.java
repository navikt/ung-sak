package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORESLÅ_VEDTAK;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@BehandlingStegRef(value = FORESLÅ_VEDTAK)
@BehandlingTypeRef(BehandlingType.REVURDERING) //Revurdering
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakRevurderingStegImpl implements ForeslåVedtakSteg {

    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<ErEndringIBeregningVurderer> endringIBeregningTjenester;
    private FormidlingDokumentdataTjeneste formidlingDokumentdataTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    ForeslåVedtakRevurderingStegImpl() {
    }

    @Inject
    ForeslåVedtakRevurderingStegImpl(ForeslåVedtakTjeneste foreslåVedtakTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider,
                                     @Any Instance<ErEndringIBeregningVurderer> endringIBeregningTjenester,
                                     FormidlingDokumentdataTjeneste formidlingDokumentdataTjeneste,
                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
        this.endringIBeregningTjenester = endringIBeregningTjenester;
        this.formidlingDokumentdataTjeneste = formidlingDokumentdataTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling revurdering = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Behandling orginalBehandling = getOriginalBehandling(revurdering);
        var revurderingRef = BehandlingReferanse.fra(revurdering);
        var originalRef = BehandlingReferanse.fra(orginalBehandling);
        BehandleStegResultat behandleStegResultat = foreslåVedtakTjeneste.foreslåVedtak(revurdering, kontekst);

        validerHarVilkårsperioder(revurdering);

        //Oppretter aksjonspunkt dersom revurdering har mindre beregningsgrunnlag enn orginal
        NavigableSet<LocalDate> skjæringstidspunkter = vilkårResultatRepository.hent(revurdering.getId())
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .collect(Collectors.toCollection(TreeSet::new));

        //TODO spørre Espen V om dette er fjernet, og flyttet til beregning
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

    private void validerHarVilkårsperioder(Behandling behandling) {
        if (vilkårsPerioderTilVurderingTjeneste == null) {
            return;
        }
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType());

        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        List<VilkårPeriode> vilkårPerioder = new ArrayList<>();
        for (var v : definerendeVilkår) {
            vilkårPerioder.addAll(vilkårResultatRepository.hent(behandling.getId())
                .getVilkår(v)
                .map(Vilkår::getPerioder)
                .orElse(Collections.emptyList()));
        }
        if (vilkårPerioder.isEmpty()) {
            throw new IllegalStateException("Fant ingen vilkårsperiode for definerende vilkår");
        }
        var oppfyltePerioder = vilkårPerioder.stream().filter(p -> p.getUtfall().equals(Utfall.OPPFYLT)).map(VilkårPeriode::getPeriode).toList();
        var rådataForVilkårsperioder = perioderTilVurderingTjeneste.utledRådataTilUtledningAvVilkårsperioder(behandling.getId());
        rådataForVilkårsperioder.entrySet()
            .forEach(e -> validerHarAllePerioder(behandling, e.getKey(), finnForventedePerioderForVurdering(e, definerendeVilkår, oppfyltePerioder)));

    }

    private static NavigableSet<DatoIntervallEntitet> finnForventedePerioderForVurdering(Map.Entry<VilkårType, NavigableSet<DatoIntervallEntitet>> e, Set<VilkårType> definerendeVilkår, List<DatoIntervallEntitet> oppfyltePerioder) {
        if (erDefinerendeVilkår(e, definerendeVilkår)) {
            return e.getValue();
        }
        var rådataTidslinje = TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(e.getValue());
        var oppfyltePerioderTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(oppfyltePerioder);
        var tidslinjeSomSkulleHaBlittVurdert = rådataTidslinje.intersection(oppfyltePerioderTidslinje);
        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeSomSkulleHaBlittVurdert);
    }

    private void validerHarAllePerioder(Behandling behandling, VilkårType type, NavigableSet<DatoIntervallEntitet> forventetPerioder) {
        var vilkårsperioderTidslinje = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(type)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
                .map(p -> new LocalDateTimeline<>(p.getFom(), p.getTom(), true))
                    .reduce(LocalDateTimeline::crossJoin)
                        .orElse(LocalDateTimeline.empty());

        var forventetPeriodeTidslinje = TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(forventetPerioder);

        if (!forventetPeriodeTidslinje.disjoint(vilkårsperioderTidslinje).isEmpty()) {
            throw new IllegalStateException("Det finnes forventede perioder som ikke finnes i vilkårsresultat");

        }
    }

    private static boolean erDefinerendeVilkår(Map.Entry<VilkårType, NavigableSet<DatoIntervallEntitet>> e, Set<VilkårType> definerendeVilkår) {
        return definerendeVilkår.contains(e.getKey());
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
        if (!FORESLÅ_VEDTAK.equals(tilSteg)) {
            formidlingDokumentdataTjeneste.ryddVedTilbakehopp(kontekst.getBehandlingId());
        }
    }
}
