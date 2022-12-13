package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkKravstillerType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

class MapFraÅrskvantumResultat {
    private static final Set<Vilkår> VILKÅR_FOR_SPESIFIKT_KRAV_ELLER_AKTIVITET = Set.of(
        Vilkår.ARBEIDSFORHOLD,
        Vilkår.NYOPPSTARTET_HOS_ARBEIDSGIVER,
        Vilkår.SMITTEVERN,  //ulike regler for refusjon/direkte utbetaling. Kan få avslag for direkte ubetaling. Teoretisk pr arbeidsforhold
        Vilkår.ANDRE_SKAL_DEKKE_DAGENE, // PR ARBEIDSFORHOLD, GJELDER KUN SØKNAD
        Vilkår.FREMTIDIG_KRAV,
        Vilkår.FRAVÆR_FRA_ARBEID
    );

    public static LocalDateTimeline<UttakResultatPeriode> getTimeline(List<Aktivitet> aktiviteter) {
        List<LocalDateSegment<UttakResultatPeriode>> segmenter = new ArrayList<>();
        for (Aktivitet aktivitet : aktiviteter) {
            for (Uttaksperiode p : aktivitet.getUttaksperioder()) {
                segmenter.add(new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), mapTilUttaksAktiviteter(p, aktivitet.getArbeidsforhold())));
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

    private static Map<Vilkår, VilkårUtfall> krevLike(Map<Vilkår, VilkårUtfall> lhs, Map<Vilkår, VilkårUtfall> rhs) {
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

    public static LocalDateTimeline<Map<Vilkår, VilkårUtfall>> mapVilkår(List<Aktivitet> aktiviteter) {
        List<LocalDateSegment<Map<Vilkår, VilkårUtfall>>> segmenter = new ArrayList<>();
        for (Aktivitet aktivitet : aktiviteter) {
            for (Uttaksperiode uttaksperiode : aktivitet.getUttaksperioder()) {
                segmenter.add(new LocalDateSegment<>(uttaksperiode.getPeriode().getFom(), uttaksperiode.getPeriode().getTom(), mapVilkår(uttaksperiode, aktivitet)));
            }
        }
        return new LocalDateTimeline<>(segmenter, (intervall, lhs, rhs) -> new LocalDateSegment<>(intervall, krevLike(lhs.getValue(), rhs.getValue())));
    }

    private static Map<Vilkår, Utfall> mapVilkårPrAktivitet(Uttaksperiode uttaksperiode) {
        Map<Vilkår, Utfall> prAktivitetVilkårFraÅrskvantum = new EnumMap<>(Vilkår.class);
        for (Vilkår vilkår : VILKÅR_FOR_SPESIFIKT_KRAV_ELLER_AKTIVITET) {
            if (uttaksperiode.getVurderteVilkår().getVilkår().containsKey(vilkår)) {
                prAktivitetVilkårFraÅrskvantum.put(vilkår, uttaksperiode.getVurderteVilkår().getVilkår().get(vilkår));
            }
        }
        return prAktivitetVilkårFraÅrskvantum;
    }

    private static Map<Vilkår, VilkårUtfall> mapVilkår(Uttaksperiode uttaksperiode, Aktivitet aktivitet) {
        Map<Vilkår, VilkårUtfall> resultat = new HashMap<>();
        for (Map.Entry<Vilkår, Utfall> e : uttaksperiode.getVurderteVilkår().getVilkår().entrySet()) {
            if (e.getKey() == Vilkår.INNGANGSVILKÅR){
                continue; //dette er en samling av vilkår fra k9-sak. Ignoreres her (sendes pr vilkår et annet sted)
            }
            if (e.getKey() == Vilkår.UIDENTIFISERT_RAMMEVEDTAK){
                continue; //dette er ikke et vilkår, brukes bar internt for å trigge aksjonspunkt
            }
            VilkårUtfall vilkårUtfall = VILKÅR_FOR_SPESIFIKT_KRAV_ELLER_AKTIVITET.contains(e.getKey())
                ? new VilkårUtfall(map(e.getValue()), Set.of(mapDetaljertUtfall(e.getKey(), map(e.getValue()), aktivitet)))
                : new VilkårUtfall(map(e.getValue()));
            resultat.put(e.getKey(), vilkårUtfall);
        }
        return resultat;
    }

    private static DetaljertVilkårUtfall mapDetaljertUtfall(Vilkår vilkårType, no.nav.k9.kodeverk.vilkår.Utfall utfall, Aktivitet aktivitet) {
        return switch (vilkårType) {
            case ARBEIDSFORHOLD, SMITTEVERN ->
                DetaljertVilkårUtfall.forArbeidsforhold(utfall, aktivitet.getArbeidsforhold().getType(), aktivitet.getArbeidsforhold().getOrganisasjonsnummer(), aktivitet.getArbeidsforhold().getAktørId(), aktivitet.getArbeidsforhold().getArbeidsforholdId());
            case FREMTIDIG_KRAV ->
                //TODO denne er strengt talt pr kravstiller i tillegg til arbeidsforhold, men er uklart hvordan enkelt utlede kravstiller her (uten mye jobb)
                DetaljertVilkårUtfall.forArbeidsforhold(utfall, aktivitet.getArbeidsforhold().getType(), aktivitet.getArbeidsforhold().getOrganisasjonsnummer(), aktivitet.getArbeidsforhold().getAktørId(), aktivitet.getArbeidsforhold().getArbeidsforholdId());
            case NYOPPSTARTET_HOS_ARBEIDSGIVER, ANDRE_SKAL_DEKKE_DAGENE, FRAVÆR_FRA_ARBEID ->
                DetaljertVilkårUtfall.forKravstillerPerArbeidsforhold(utfall, StønadstatistikkKravstillerType.BRUKER, aktivitet.getArbeidsforhold().getType(), aktivitet.getArbeidsforhold().getOrganisasjonsnummer(), aktivitet.getArbeidsforhold().getAktørId(), aktivitet.getArbeidsforhold().getArbeidsforholdId());
            default -> throw new IllegalArgumentException("ikke-støttet vilkårtype: " + vilkårType);
        };
    }

    private static no.nav.k9.kodeverk.vilkår.Utfall map(Utfall value) {
        return switch (value) {
            case INNVILGET -> no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT;
            case AVSLÅTT -> no.nav.k9.kodeverk.vilkår.Utfall.IKKE_OPPFYLT;
            default -> throw new IllegalArgumentException("Ikke-støttet utfall: " + value);
        };
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
