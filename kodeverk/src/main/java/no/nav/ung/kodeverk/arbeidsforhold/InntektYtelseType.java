package no.nav.ung.kodeverk.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

public enum InntektYtelseType implements Kodeverdi {

    // Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    AAP("Arbeidsavklaringspenger", Kategori.YTELSE, OverordnetYtelseType.ARBEIDSAVKLARINGSPENGER),
    DAGPENGER("Dagpenger arbeid og hyre", Kategori.YTELSE, OverordnetYtelseType.DAGPENGER),
    FORELDREPENGER("Foreldrepenger", Kategori.YTELSE, OverordnetYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER("Svangerskapspenger", Kategori.YTELSE, OverordnetYtelseType.SVANGERSKAPSPENGER),
    SYKEPENGER("Sykepenger", Kategori.YTELSE, OverordnetYtelseType.SYKEPENGER),
    OMSORGSPENGER("Omsorgspenger", Kategori.YTELSE, OverordnetYtelseType.OMSORGSPENGER),
    OPPLÆRINGSPENGER("Opplæringspenger", Kategori.YTELSE, OverordnetYtelseType.OPPLÆRINGSPENGER),
    PLEIEPENGER("Pleiepenger", Kategori.YTELSE, OverordnetYtelseType.PLEIEPENGER),
    OVERGANGSSTØNAD_ENSLIG("Overgangsstønad til enslig mor eller far", Kategori.YTELSE, OverordnetYtelseType.ENSLIG_FORSØRGER),
    VENTELØNN("Ventelønn", Kategori.YTELSE, OverordnetYtelseType.UDEFINERT),

    // Feriepenger Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    // TODO slå sammen til FERIEPENGER_YTELSE - eller ta de med under hver ytelse???
    FERIEPENGER_FORELDREPENGER("Feriepenger foreldrepenger", Kategori.YTELSE, OverordnetYtelseType.FORELDREPENGER),
    FERIEPENGER_SVANGERSKAPSPENGER("Feriepenger svangerskapspenger", Kategori.YTELSE, OverordnetYtelseType.SVANGERSKAPSPENGER),
    FERIEPENGER_OMSORGSPENGER("Feriepenger omsorgspenger", Kategori.YTELSE, OverordnetYtelseType.OMSORGSPENGER),
    FERIEPENGER_OPPLÆRINGSPENGER("Feriepenger opplæringspenger", Kategori.YTELSE, OverordnetYtelseType.OPPLÆRINGSPENGER),
    FERIEPENGER_PLEIEPENGER("Feriepenger pleiepenger", Kategori.YTELSE, OverordnetYtelseType.PLEIEPENGER),
    FERIEPENGER_SYKEPENGER("Feriepenger sykepenger", Kategori.YTELSE, OverordnetYtelseType.SYKEPENGER),
    FERIETILLEGG_DAGPENGER("Ferietillegg dagpenger ", Kategori.YTELSE, OverordnetYtelseType.DAGPENGER),

    // Annen ytelse utbetalt til person
    KVALIFISERINGSSTØNAD("Kvalifiseringsstønad", Kategori.TRYGD, OverordnetYtelseType.UDEFINERT),

    // Ytelse utbetalt til person som er næringsdrivende, fisker/lott, dagmamma eller jord/skogbruker
    FORELDREPENGER_NÆRING("Foreldrepenger næring", Kategori.NÆRING, OverordnetYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER_NÆRING("Svangerskapspenger næring", Kategori.NÆRING, OverordnetYtelseType.SVANGERSKAPSPENGER),
    SYKEPENGER_NÆRING("Sykepenger næring", Kategori.NÆRING, OverordnetYtelseType.SYKEPENGER),
    OMSORGSPENGER_NÆRING("Omsorgspenger næring", Kategori.NÆRING, OverordnetYtelseType.OMSORGSPENGER),
    OPPLÆRINGSPENGER_NÆRING("Opplæringspenger næring", Kategori.NÆRING, OverordnetYtelseType.OPPLÆRINGSPENGER),
    PLEIEPENGER_NÆRING("Pleiepenger næring", Kategori.NÆRING, OverordnetYtelseType.PLEIEPENGER),
    DAGPENGER_NÆRING("Dagpenger næring", Kategori.NÆRING, OverordnetYtelseType.DAGPENGER),

    // Annen ytelse utbetalt til person som er næringsdrivende
    ANNET("Annet", Kategori.NÆRING, OverordnetYtelseType.UDEFINERT),
    VEDERLAG("Vederlag", Kategori.NÆRING, OverordnetYtelseType.UDEFINERT),
    LOTT_KUN_TRYGDEAVGIFT("Lott kun trygdeavgift", Kategori.NÆRING, OverordnetYtelseType.UDEFINERT),
    KOMPENSASJON_FOR_TAPT_PERSONINNTEKT("Kompensasjon for tapt personinntekt", Kategori.NÆRING, OverordnetYtelseType.UDEFINERT)
    ;

    public static final String KODEVERK = "INNTEKT_YTELSE_TYPE";

    private final String navn;
    private final OverordnetYtelseType overordnetYtelseType;
    private final Kategori kategori;

    InntektYtelseType(String navn, Kategori kategori, OverordnetYtelseType overordnetYtelseType) {
        this.navn = navn;
        this.kategori = kategori;
        this.overordnetYtelseType = overordnetYtelseType;
    }

    public static InntektYtelseType fraKode(String kode) {
        return kode != null ? InntektYtelseType.valueOf(kode) : null;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    @JsonValue
    public String getKode() {
        return name();
    }

    @Override
    public String getOffisiellKode() {
        return null;
    }

    public OverordnetYtelseType getOverordnetYtelseType() {
        return overordnetYtelseType;
    }

    private boolean erOrdinærYtelse() {
        return kategori == Kategori.YTELSE;
    }

    private boolean erNæringsYtelse() {
        return kategori == Kategori.NÆRING;
    }

    public enum Kategori { YTELSE, NÆRING, TRYGD }


}
