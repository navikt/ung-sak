package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.opptjening.MellomliggendeHelgUtleder;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

public final class MapYrkesaktivitetTilOpptjeningsperiodeTjeneste {

    private static final MellomliggendeHelgUtleder MELLOMLIGGENDE_HELG_UTLEDER = new MellomliggendeHelgUtleder();

    private MapYrkesaktivitetTilOpptjeningsperiodeTjeneste() {
    }

    public static List<OpptjeningsperiodeForSaksbehandling> mapYrkesaktivitet(BehandlingReferanse behandlingReferanse,
                                                                              Yrkesaktivitet registerAktivitet,
                                                                              InntektArbeidYtelseGrunnlag grunnlag,
                                                                              OpptjeningAktivitetVurdering vurderForSaksbehandling,
                                                                              Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
                                                                              DatoIntervallEntitet opptjeningsperiode,
                                                                              DatoIntervallEntitet vilkårsPeriode,
                                                                              Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, registerAktivitet.getArbeidType());
        return new ArrayList<>(mapAktivitetsavtaler(behandlingReferanse, registerAktivitet, grunnlag,
            vurderForSaksbehandling, type, opptjeningsperiode, vilkårsPeriode, tidslinjePerYtelse));
    }

    private static OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
            .stream()
            .findFirst()
            .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private static List<OpptjeningsperiodeForSaksbehandling> mapAktivitetsavtaler(BehandlingReferanse behandlingReferanse,
                                                                                  Yrkesaktivitet registerAktivitet,
                                                                                  InntektArbeidYtelseGrunnlag grunnlag,
                                                                                  OpptjeningAktivitetVurdering vurderForSaksbehandling,
                                                                                  OpptjeningAktivitetType type,
                                                                                  DatoIntervallEntitet opptjeningsperiode,
                                                                                  DatoIntervallEntitet vilkårsperiode,
                                                                                  Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse) {
        List<OpptjeningsperiodeForSaksbehandling> perioderForAktivitetsavtaler = new ArrayList<>();
        LocalDate skjæringstidspunkt = vilkårsperiode.getFomDato();
        var permisjonstidslinje = utledPermisjonstidslinjeForPerioden(registerAktivitet, tidslinjePerYtelse, vilkårsperiode, opptjeningsperiode);
        for (AktivitetsAvtale avtale : gjeldendeAvtaler(grunnlag, skjæringstidspunkt, registerAktivitet)) {
            var perioder = utledPerioderEtterÅTattHensynTilPermisjoner(permisjonstidslinje, avtale);
            for (DatoIntervallEntitet periode : perioder) {
                var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                    .medOpptjeningAktivitetType(type)
                    .medPeriode(periode)
                    .medBegrunnelse(avtale.getBeskrivelse())
                    .medStillingsandel(finnStillingsprosent(registerAktivitet, skjæringstidspunkt));
                settArbeidsgiverInformasjon(registerAktivitet, builder);
                var input = new VurderStatusInput(type, behandlingReferanse);
                input.setRegisterAktivitet(registerAktivitet);
                input.setAktivitetPeriode(periode);
                input.setTidslinjePerYtelse(tidslinjePerYtelse);
                input.setVilkårsperiode(vilkårsperiode);
                input.setOpptjeningsperiode(opptjeningsperiode);
                builder.medVurderingsStatus(vurderForSaksbehandling.vurderStatus(input));
                perioderForAktivitetsavtaler.add(builder.build());
            }
        }
        return perioderForAktivitetsavtaler;
    }

    private static LocalDateTimeline<Boolean> utledPermisjonstidslinjeForPerioden(Yrkesaktivitet yrkesaktivitet,
                                                                                  Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse, DatoIntervallEntitet vilkårsperiode,
                                                                                  DatoIntervallEntitet opptjeningsperiode) {
        LocalDateTimeline<Boolean> aktivPermisjonTidslinje = PermisjonPerYrkesaktivitet.utledPermisjonPerYrkesaktivitet(yrkesaktivitet, tidslinjePerYtelse, vilkårsperiode);

        // Vurder kun permisjonsperioder som overlapper opptjeningsperiode
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(opptjeningsperiode.getFomDato(), opptjeningsperiode.getTomDato(), Boolean.FALSE)));
        tidslinjeTilVurdering = tidslinjeTilVurdering.intersection(aktivPermisjonTidslinje.compress());

        // Legg til mellomliggende periode dersom helg mellom permisjonsperioder
        LocalDateTimeline<Boolean> mellomliggendePerioder = MELLOMLIGGENDE_HELG_UTLEDER.beregnMellomliggendeHelg(tidslinjeTilVurdering);
        for (LocalDateSegment<Boolean> mellomliggendePeriode : mellomliggendePerioder) {
            tidslinjeTilVurdering = tidslinjeTilVurdering.combine(mellomliggendePeriode, MapYrkesaktivitetTilOpptjeningsperiodeTjeneste::mergePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return tidslinjeTilVurdering.compress();
    }

    private static LocalDateSegment<Boolean> mergePerioder(LocalDateInterval di, LocalDateSegment<Boolean> førsteVersjon, LocalDateSegment<Boolean> sisteVersjon) {

        if ((førsteVersjon == null || førsteVersjon.getValue() == null) && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if ((sisteVersjon == null || sisteVersjon.getValue() == null) && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();
        return lagSegment(di, første || siste);
    }

    private static Set<DatoIntervallEntitet> utledPerioderEtterÅTattHensynTilPermisjoner(LocalDateTimeline<Boolean> permisjonsTidslinje, AktivitetsAvtale avtale) {
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(avtale.getPeriode().toLocalDateInterval(), true)));

        timeline = timeline.combine(permisjonsTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return TidslinjeUtil.tilDatoIntervallEntiteter(timeline.compress());
    }

    private static LocalDateSegment<Boolean> lagSegment(LocalDateInterval di, Boolean siste) {
        if (siste == null) {
            return new LocalDateSegment<>(di, null);
        }
        return new LocalDateSegment<>(di, siste);
    }

    public static void settArbeidsgiverInformasjon(Yrkesaktivitet yrkesaktivitet, OpptjeningsperiodeForSaksbehandling.Builder builder) {
        Arbeidsgiver arbeidsgiver = yrkesaktivitet.getArbeidsgiver();
        if (arbeidsgiver != null) {
            builder.medArbeidsgiver(arbeidsgiver);
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(yrkesaktivitet.getArbeidsforholdRef(), arbeidsgiver));
        }
        if (yrkesaktivitet.getNavnArbeidsgiverUtland() != null) {
            builder.medArbeidsgiverUtlandNavn(yrkesaktivitet.getNavnArbeidsgiverUtland());
        }
    }

    private static Stillingsprosent finnStillingsprosent(Yrkesaktivitet registerAktivitet, LocalDate skjæringstidspunkt) {
        final Stillingsprosent defaultStillingsprosent = new Stillingsprosent(0);
        if (registerAktivitet.erArbeidsforhold()) {
            var filter = new YrkesaktivitetFilter(null, List.of(registerAktivitet));
            return filter.getAktivitetsAvtalerForArbeid()
                .stream()
                .filter(aa -> aa.getProsentsats() != null)
                .filter(aa -> aa.getPeriode().inkluderer(skjæringstidspunkt))
                .map(AktivitetsAvtale::getProsentsats)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Stillingsprosent::getVerdi))
                .orElse(defaultStillingsprosent);
        }
        return defaultStillingsprosent;
    }

    private static Collection<AktivitetsAvtale> gjeldendeAvtaler(InntektArbeidYtelseGrunnlag grunnlag,
                                                                 LocalDate skjæringstidspunktForOpptjening,
                                                                 Yrkesaktivitet registerAktivitet) {
        if (registerAktivitet.erArbeidsforhold()) {
            var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), registerAktivitet);

            return filter.getAnsettelsesPerioder(registerAktivitet);
        }
        return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), registerAktivitet)
            .før(skjæringstidspunktForOpptjening)
            .getAktivitetsAvtalerForArbeid();
    }

}
