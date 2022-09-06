package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

class MapFraÅrskvantumResultat {

    //styrer om vilkår mappes direkte på UttakResultatPeriode, eller i UttakAktivitet
    private static final Set<Vilkår> VILKÅR_PR_AKTIVITET = Set.of(Vilkår.ARBEIDSFORHOLD, Vilkår.NYOPPSTARTET_HOS_ARBEIDSGIVER); //TODO komplett liste?

    public static LocalDateTimeline<UttakResultatPeriode> getTimeline(List<Aktivitet> aktiviteter) {
        List<LocalDateSegment<UttakResultatPeriode>> segmenter = new ArrayList<>();
        for (Aktivitet aktivitet : aktiviteter) {
            for (Uttaksperiode p : aktivitet.getUttaksperioder()) {
                segmenter.add(new LocalDateSegment<UttakResultatPeriode>(p.getPeriode().getFom(), p.getPeriode().getTom(), mapTilUttaksAktiviteter(p, aktivitet.getArbeidsforhold())));
            }
        }
        return new LocalDateTimeline<>(segmenter, (intervall, lhs, rhs) -> new LocalDateSegment<>(intervall, kombiner(lhs.getValue(), rhs.getValue())));
    }

    private static UttakResultatPeriode kombiner(UttakResultatPeriode lhs, UttakResultatPeriode rhs) {
        List<UttakAktivitet> aktiviteter = concat(lhs.getUttakAktiviteter(), rhs.getUttakAktiviteter());
        return new UttakResultatPeriode(aktiviteter);
    }

    private static List<UttakAktivitet> concat(List<UttakAktivitet> lhs, List<UttakAktivitet> rhs) {
        List<UttakAktivitet> alle = new ArrayList<>();
        alle.addAll(lhs);
        alle.addAll(rhs);
        return alle;
    }

    private static Map<Vilkår, Utfall> krevLike(Map<Vilkår, Utfall> lhs, Map<Vilkår, Utfall> rhs) {
        if (lhs.equals(rhs)) {
            return lhs;
        }
        throw new IllegalArgumentException("Argumentene var ikke like: " + lhs + " og " + rhs);
    }

    private static UttakResultatPeriode mapTilUttaksAktiviteter(Uttaksperiode uttaksperiode, no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold uttakArbeidsforhold) {
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Omsorgspenger, bruker kun utbetalingsgrad

        Map<Vilkår, Utfall> vilkårPrAktivitet = mapVilkårPrAktivitet(uttaksperiode);
        UttakAktivitet aktivitet;
        if (uttaksperiode.getUtbetalingsgrad() == null) {
            aktivitet = new UttakAktivitet(stillingsgrad, BigDecimal.ZERO, null, null, vilkårPrAktivitet);
        } else {
            var utbetalingsgrad = uttaksperiode.getUtbetalingsgrad();
            var arbeidsforhold = mapArbeidsforhold(uttakArbeidsforhold);
            aktivitet = new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, UttakArbeidType.fraKode(uttakArbeidsforhold.getType()), vilkårPrAktivitet);
        }
        return new UttakResultatPeriode(List.of(aktivitet));
    }

    public static LocalDateTimeline<Map<Vilkår, Utfall>> mapVilkårSomGjelderAlleAktiviteter(List<Aktivitet> aktiviteter){
        List<LocalDateSegment<Map<Vilkår, Utfall>>> segmenter = new ArrayList<>();
        for (Aktivitet aktivitet : aktiviteter) {
            for (Uttaksperiode uttaksperiode : aktivitet.getUttaksperioder()) {
                segmenter.add(new LocalDateSegment<>(uttaksperiode.getPeriode().getFom(), uttaksperiode.getPeriode().getTom(),mapVilkårSomGjelderAlleAktiviteter(uttaksperiode)));
            }
        }
        return new LocalDateTimeline<>(segmenter, (intervall, lhs, rhs) -> new LocalDateSegment<>(intervall, krevLike(lhs.getValue(), rhs.getValue())));
    }

    private static Map<Vilkår, Utfall> mapVilkårPrAktivitet(Uttaksperiode uttaksperiode) {
        Map<Vilkår, Utfall> prAktivitetVilkårFraÅrskvantum = new EnumMap<>(Vilkår.class);
        for (Vilkår vilkår : VILKÅR_PR_AKTIVITET) {
            if (uttaksperiode.getVurderteVilkår().getVilkår().containsKey(vilkår)) {
                prAktivitetVilkårFraÅrskvantum.put(vilkår, uttaksperiode.getVurderteVilkår().getVilkår().get(vilkår));
            }
        }
        return prAktivitetVilkårFraÅrskvantum;
    }

    @NotNull
    private static Map<Vilkår, Utfall> mapVilkårSomGjelderAlleAktiviteter(Uttaksperiode uttaksperiode) {
        Map<Vilkår, Utfall> påTversAvAktiviteterVilkårFraÅrskvantum = new EnumMap<>(Vilkår.class);
        påTversAvAktiviteterVilkårFraÅrskvantum.putAll(uttaksperiode.getVurderteVilkår().getVilkår());
        påTversAvAktiviteterVilkårFraÅrskvantum.remove(Vilkår.INNGANGSVILKÅR); //dette er en samlig av vilkår, skal heller samle faktiske inngangsvilkår fra k9sak
        for (Vilkår vilkår : VILKÅR_PR_AKTIVITET) {
            påTversAvAktiviteterVilkårFraÅrskvantum.remove(vilkår);
        }
        return påTversAvAktiviteterVilkårFraÅrskvantum;
    }

    static Arbeidsforhold mapArbeidsforhold(no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold arb) {
        final Arbeidsforhold.Builder arbeidsforholdBuilder = Arbeidsforhold.builder();
        if (arb.getOrganisasjonsnummer() != null) {
            arbeidsforholdBuilder.medOrgnr(arb.getOrganisasjonsnummer());
        } else if (arb.getAktørId() != null) {
            arbeidsforholdBuilder.medAktørId(arb.getAktørId());
        }
        arbeidsforholdBuilder.medArbeidsforholdId(arb.getArbeidsforholdId());
        arbeidsforholdBuilder.medFrilanser("FL".equals(arb.getType()));
        return arbeidsforholdBuilder.build();
    }


}
