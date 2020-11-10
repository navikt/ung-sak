package no.nav.k9.sak.ytelse.unntaksbehandling.vilkår;

import static java.lang.String.format;
import static java.util.Map.entry;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

public class InntektskategoriTilAktivitetstatusMapper {

    private static final Map<Inntektskategori, AktivitetStatus> INNTEKTSKATEGORI_AKTIVITET_STATUS_MAP = Map.ofEntries(
        entry(Inntektskategori.ARBEIDSTAKER,/*                  */ AktivitetStatus.ARBEIDSTAKER),
        entry(Inntektskategori.FRILANSER,/*                     */ AktivitetStatus.FRILANSER),
        entry(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,/*   */ AktivitetStatus.FRILANSER),
        entry(Inntektskategori.DAGPENGER,/*                     */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
        entry(Inntektskategori.ARBEIDSAVKLARINGSPENGER,/*       */ AktivitetStatus.DAGPENGER),
        entry(Inntektskategori.SJØMANN,/*                       */ AktivitetStatus.ARBEIDSAVKLARINGSPENGER),
        entry(Inntektskategori.DAGMAMMA,/*                      */ AktivitetStatus.ARBEIDSTAKER),
        entry(Inntektskategori.JORDBRUKER,/*                    */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
        entry(Inntektskategori.FISKER,/*                        */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
        entry(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER,/* */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
    );

    public static AktivitetStatus aktivitetStatusFor(Inntektskategori inntektskategori) {
        return INNTEKTSKATEGORI_AKTIVITET_STATUS_MAP.entrySet().stream()
            .filter(e -> e.getKey().equals(inntektskategori))
            .collect(toSingleton(inntektskategori))
            .getValue();
    }

    public static <T> Collector<T, ?, T> toSingleton(Inntektskategori inntektskategori) {
        return Collectors.collectingAndThen(
            Collectors.toList(),
            list -> {
                if (list.size() != 1) {
                    throw new IllegalArgumentException(format("Mangler mapping for inntektskategori: %s", inntektskategori));
                }
                return list.get(0);
            }
        );
    }
}

