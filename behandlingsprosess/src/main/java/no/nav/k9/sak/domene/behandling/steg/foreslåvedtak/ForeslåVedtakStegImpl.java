package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORESLÅ_VEDTAK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.dokument.bestill.tjenester.FormidlingDokumentdataTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@BehandlingStegRef(value = FORESLÅ_VEDTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakStegImpl implements ForeslåVedtakSteg {

    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private FormidlingDokumentdataTjeneste formidlingDokumentdataTjeneste;
    private Instance<YtelsespesifikkForeslåVedtak> ytelsespesifikkForeslåVedtak;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    ForeslåVedtakStegImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåVedtakStegImpl(BehandlingRepository behandlingRepository,
                          ForeslåVedtakTjeneste foreslåVedtakTjeneste,
                          FormidlingDokumentdataTjeneste formidlingDokumentdataTjeneste,
                          @Any Instance<YtelsespesifikkForeslåVedtak> ytelsespesifikkForeslåVedtak,
                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste, VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
        this.formidlingDokumentdataTjeneste = formidlingDokumentdataTjeneste;
        this.ytelsespesifikkForeslåVedtak = ytelsespesifikkForeslåVedtak;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        validerHarVilkårsperioder(behandling);

        final Optional<BehandleStegResultat> ytelsespesifikkForeslåVedtakResultat = hentAlternativForeslåVedtak(behandling)
            .map(afv -> afv.run(BehandlingReferanse.fra(behandling)));
        if (ytelsespesifikkForeslåVedtakResultat.isPresent()) {
            return ytelsespesifikkForeslåVedtakResultat.get();
        }

        return foreslåVedtakTjeneste.foreslåVedtak(behandling, kontekst);
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


    private Optional<? extends YtelsespesifikkForeslåVedtak> hentAlternativForeslåVedtak(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(ytelsespesifikkForeslåVedtak, behandling.getFagsakYtelseType());
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!FORESLÅ_VEDTAK.equals(tilSteg)) {
            formidlingDokumentdataTjeneste.ryddVedTilbakehopp(kontekst.getBehandlingId());
        }
    }
}
