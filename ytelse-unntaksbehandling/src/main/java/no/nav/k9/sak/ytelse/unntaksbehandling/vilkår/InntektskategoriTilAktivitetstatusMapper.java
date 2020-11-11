package no.nav.k9.sak.ytelse.unntaksbehandling.vilkår;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;

import java.util.Map;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

public class InntektskategoriTilAktivitetstatusMapper {

    private static final Map<Inntektskategori, AktivitetStatus> INNTEKTSKATEGORI_AKTIVITET_STATUS_MAP = Map.ofEntries(
        entry(Inntektskategori.ARBEIDSTAKER,/*                  */ AktivitetStatus.ARBEIDSTAKER),
        entry(Inntektskategori.FRILANSER,/*                     */ AktivitetStatus.FRILANSER),
        entry(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,/*   */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
        entry(Inntektskategori.DAGPENGER,/*                     */ AktivitetStatus.DAGPENGER),
        entry(Inntektskategori.ARBEIDSAVKLARINGSPENGER,/*       */ AktivitetStatus.ARBEIDSAVKLARINGSPENGER),
        entry(Inntektskategori.SJØMANN,/*                       */ AktivitetStatus.ARBEIDSTAKER),
        entry(Inntektskategori.DAGMAMMA,/*                      */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
        entry(Inntektskategori.JORDBRUKER,/*                    */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
        entry(Inntektskategori.FISKER,/*                        */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
        entry(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER,/* */ AktivitetStatus.ARBEIDSTAKER)
    );

    public static AktivitetStatus aktivitetStatusFor(Inntektskategori inntektskategori) {
        return ofNullable(INNTEKTSKATEGORI_AKTIVITET_STATUS_MAP.get(inntektskategori))
            .orElseThrow(() -> new IllegalArgumentException(format("Mangler mapping for inntektskategori: %s", inntektskategori)));

    }
}

