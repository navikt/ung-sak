package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.perioder.*;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
@FagsakYtelseTypeRef("OMP")
public class OMPSøknadsfristTjeneste implements SøknadsfristTjeneste {

    private VurderSøknadsfristTjeneste<OppgittFraværPeriode> tjeneste;

    @Inject
    public OMPSøknadsfristTjeneste(@FagsakYtelseTypeRef("OMP") VurderSøknadsfristTjeneste<OppgittFraværPeriode> tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public VilkårResultatBuilder vurderSøknadsfrist(BehandlingReferanse referanse, VilkårResultatBuilder vilkårResultatBuilder) {
        var søktePerioder = tjeneste.hentPerioderTilVurdering(referanse);
        var vurdertePerioder = tjeneste.vurderSøknadsfrist(søktePerioder);

        return mapVurderingerTilVilkårsresultat(vilkårResultatBuilder, søktePerioder, vurdertePerioder, referanse.getFagsakPeriode());
    }

    VilkårResultatBuilder mapVurderingerTilVilkårsresultat(VilkårResultatBuilder vilkårResultatBuilder,
                                                           Map<Søknad, Set<SøktPeriode<OppgittFraværPeriode>>> søktePerioder,
                                                           Map<Søknad, Set<VurdertSøktPeriode<OppgittFraværPeriode>>> vurdertePerioder,
                                                           DatoIntervallEntitet fagsakPeriode) {
        // Oversett til vilkårmodell
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.SØKNADSFRIST)
            .tilbakestill(fagsakPeriode);
        var vilkårTimeline = slåSammenTidslinjer(vurdertePerioder);

        vilkårTimeline.toSegments()
            .forEach(it -> leggInnVurdering(vilkårBuilder, it, søktePerioder, vurdertePerioder));

        return vilkårResultatBuilder.leggTil(vilkårBuilder);
    }

    private void leggInnVurdering(VilkårBuilder vilkårBuilder, LocalDateSegment<Utfall> it,
                                  Map<Søknad, Set<SøktPeriode<OppgittFraværPeriode>>> søktePerioder,
                                  Map<Søknad, Set<VurdertSøktPeriode<OppgittFraværPeriode>>> vurdertePerioder) {
        Utfall utfall = it.getValue();
        VilkårPeriodeBuilder builder = vilkårBuilder.hentBuilderFor(it.getFom(), it.getTom())
            .medUtfall(utfall);

        try {
            builder.medRegelInput(JsonObjectMapper.getJson(søktePerioder))
                .medRegelEvaluering(JsonObjectMapper.getJson(vurdertePerioder));
        } catch (IOException e) {
            throw new IllegalArgumentException("Feiler på serialisering av regelsporing");
        }

        if (Utfall.IKKE_OPPFYLT.equals(utfall)) {
            builder.medAvslagsårsak(Avslagsårsak.SØKT_FOR_SENT);
        }

        vilkårBuilder.leggTil(builder);
    }

    private LocalDateTimeline<Utfall> slåSammenTidslinjer(Map<Søknad, Set<VurdertSøktPeriode<OppgittFraværPeriode>>> vurdertePerioder) {
        var vilkårTimeline = new LocalDateTimeline<Utfall>(List.of());
        var timelines = vurdertePerioder.values()
            .stream()
            .map(this::mapTilTimeline).collect(Collectors.toList());
        for (LocalDateTimeline<Utfall> timeline : timelines) {
            vilkårTimeline = mergeTimeline(vilkårTimeline, timeline);
        }
        return vilkårTimeline.compress();
    }

    private LocalDateTimeline<Utfall> mergeTimeline(LocalDateTimeline<Utfall> vilkårTimeline, LocalDateTimeline<Utfall> vurdertTimeline) {
        return vilkårTimeline.combine(vurdertTimeline, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private LocalDateSegment<Utfall> mergePeriode(LocalDateInterval di,
                                                  LocalDateSegment<Utfall> førsteVersjon,
                                                  LocalDateSegment<Utfall> sisteVersjon) {
        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }
        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();

        return lagSegment(di, prioriterUtfall(første, siste));
    }

    private LocalDateSegment<Utfall> lagSegment(LocalDateInterval di, Utfall prioriterUtfall) {
        return new LocalDateSegment<>(di, prioriterUtfall);
    }

    private Utfall prioriterUtfall(Utfall første, Utfall siste) {
        EnumSet<Utfall> values = EnumSet.of(første, siste);
        if (values.contains(Utfall.IKKE_VURDERT)) {
            return Utfall.IKKE_VURDERT;
        }
        if (values.contains(Utfall.OPPFYLT)) {
            return Utfall.OPPFYLT;
        }
        return Utfall.IKKE_OPPFYLT;
    }

    private LocalDateTimeline<Utfall> mapTilTimeline(Set<VurdertSøktPeriode<OppgittFraværPeriode>> perioder) {
        List<LocalDateSegment<Utfall>> segments = perioder.stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), it.getUtfall()))
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(segments);
    }
}
