package no.nav.ung.kodeverk.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

public enum InntektYtelseType implements Kodeverdi {

    // Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    AAP("Arbeidsavklaringspenger", Kategori.YTELSE, OverordnetInntektYtelseType.ARBEIDSAVKLARINGSPENGER),
    DAGPENGER("Dagpenger arbeid og hyre", Kategori.YTELSE, OverordnetInntektYtelseType.DAGPENGER),
    FORELDREPENGER("Foreldrepenger", Kategori.YTELSE, OverordnetInntektYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER("Svangerskapspenger", Kategori.YTELSE, OverordnetInntektYtelseType.SVANGERSKAPSPENGER),
    SYKEPENGER("Sykepenger", Kategori.YTELSE, OverordnetInntektYtelseType.SYKEPENGER),
    OMSORGSPENGER("Omsorgspenger", Kategori.YTELSE, OverordnetInntektYtelseType.OMSORGSPENGER),
    OPPLÆRINGSPENGER("Opplæringspenger", Kategori.YTELSE, OverordnetInntektYtelseType.OPPLÆRINGSPENGER),
    PLEIEPENGER("Pleiepenger", Kategori.YTELSE, OverordnetInntektYtelseType.PLEIEPENGER),
    OVERGANGSSTØNAD_ENSLIG("Overgangsstønad til enslig mor eller far", Kategori.YTELSE, OverordnetInntektYtelseType.ENSLIG_FORSØRGER),
    VENTELØNN("Ventelønn", Kategori.YTELSE, OverordnetInntektYtelseType.UDEFINERT),

    // Feriepenger Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    // TODO slå sammen til FERIEPENGER_YTELSE - eller ta de med under hver ytelse???
    FERIEPENGER_FORELDREPENGER("Feriepenger foreldrepenger", Kategori.YTELSE, OverordnetInntektYtelseType.FORELDREPENGER),
    FERIEPENGER_SVANGERSKAPSPENGER("Feriepenger svangerskapspenger", Kategori.YTELSE, OverordnetInntektYtelseType.SVANGERSKAPSPENGER),
    FERIEPENGER_OMSORGSPENGER("Feriepenger omsorgspenger", Kategori.YTELSE, OverordnetInntektYtelseType.OMSORGSPENGER),
    FERIEPENGER_OPPLÆRINGSPENGER("Feriepenger opplæringspenger", Kategori.YTELSE, OverordnetInntektYtelseType.OPPLÆRINGSPENGER),
    FERIEPENGER_PLEIEPENGER("Feriepenger pleiepenger", Kategori.YTELSE, OverordnetInntektYtelseType.PLEIEPENGER),
    FERIEPENGER_SYKEPENGER("Feriepenger sykepenger", Kategori.YTELSE, OverordnetInntektYtelseType.SYKEPENGER),
    FERIETILLEGG_DAGPENGER("Ferietillegg dagpenger ", Kategori.YTELSE, OverordnetInntektYtelseType.DAGPENGER),

    // Annen ytelse utbetalt til person
    KVALIFISERINGSSTØNAD("Kvalifiseringsstønad", Kategori.TRYGD, OverordnetInntektYtelseType.UDEFINERT),

    // Ytelse utbetalt til person som er næringsdrivende, fisker/lott, dagmamma eller jord/skogbruker
    FORELDREPENGER_NÆRING("Foreldrepenger næring", Kategori.NÆRING, OverordnetInntektYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER_NÆRING("Svangerskapspenger næring", Kategori.NÆRING, OverordnetInntektYtelseType.SVANGERSKAPSPENGER),
    SYKEPENGER_NÆRING("Sykepenger næring", Kategori.NÆRING, OverordnetInntektYtelseType.SYKEPENGER),
    OMSORGSPENGER_NÆRING("Omsorgspenger næring", Kategori.NÆRING, OverordnetInntektYtelseType.OMSORGSPENGER),
    OPPLÆRINGSPENGER_NÆRING("Opplæringspenger næring", Kategori.NÆRING, OverordnetInntektYtelseType.OPPLÆRINGSPENGER),
    PLEIEPENGER_NÆRING("Pleiepenger næring", Kategori.NÆRING, OverordnetInntektYtelseType.PLEIEPENGER),
    DAGPENGER_NÆRING("Dagpenger næring", Kategori.NÆRING, OverordnetInntektYtelseType.DAGPENGER),

    // Annen ytelse utbetalt til person som er næringsdrivende
    ANNET("Annet", Kategori.NÆRING, OverordnetInntektYtelseType.UDEFINERT),
    VEDERLAG("Vederlag", Kategori.NÆRING, OverordnetInntektYtelseType.UDEFINERT),
    LOTT_KUN_TRYGDEAVGIFT("Lott kun trygdeavgift", Kategori.NÆRING, OverordnetInntektYtelseType.UDEFINERT),
    KOMPENSASJON_FOR_TAPT_PERSONINNTEKT("Kompensasjon for tapt personinntekt", Kategori.NÆRING, OverordnetInntektYtelseType.UDEFINERT)
    ;

    public static final String KODEVERK = "INNTEKT_YTELSE_TYPE";

    private final String navn;
    private final OverordnetInntektYtelseType overordnetInntektYtelseType;
    private final Kategori kategori;

    InntektYtelseType(String navn, Kategori kategori, OverordnetInntektYtelseType overordnetInntektYtelseType) {
        this.navn = navn;
        this.kategori = kategori;
        this.overordnetInntektYtelseType = overordnetInntektYtelseType;
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

    public OverordnetInntektYtelseType getOverordnetYtelseType() {
        return overordnetInntektYtelseType;
    }

    private boolean erOrdinærYtelse() {
        return kategori == Kategori.YTELSE;
    }

    private boolean erNæringsYtelse() {
        return kategori == Kategori.NÆRING;
    }

    public enum Kategori { YTELSE, NÆRING, TRYGD }


}
