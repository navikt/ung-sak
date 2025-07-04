package no.nav.ung.kodeverk.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public enum InntektYtelseType implements Kodeverdi {

    // Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    AAP("Arbeidsavklaringspenger", Kategori.YTELSE, FagsakYtelseType.ARBEIDSAVKLARINGSPENGER),
    DAGPENGER("Dagpenger arbeid og hyre", Kategori.YTELSE, FagsakYtelseType.DAGPENGER),
    FORELDREPENGER("Foreldrepenger", Kategori.YTELSE, FagsakYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER("Svangerskapspenger", Kategori.YTELSE, FagsakYtelseType.SVANGERSKAPSPENGER),
    SYKEPENGER("Sykepenger", Kategori.YTELSE, FagsakYtelseType.SYKEPENGER),
    OMSORGSPENGER("Omsorgspenger", Kategori.YTELSE, FagsakYtelseType.OMSORGSPENGER),
    OPPLÆRINGSPENGER("Opplæringspenger", Kategori.YTELSE, FagsakYtelseType.OPPLÆRINGSPENGER),
    PLEIEPENGER("Pleiepenger", Kategori.YTELSE, FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
    OVERGANGSSTØNAD_ENSLIG("Overgangsstønad til enslig mor eller far", Kategori.YTELSE, FagsakYtelseType.ENSLIG_FORSØRGER),
    VENTELØNN("Ventelønn", Kategori.YTELSE, FagsakYtelseType.UDEFINERT),

    // Feriepenger Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    // TODO slå sammen til FERIEPENGER_YTELSE - eller ta de med under hver ytelse???
    FERIEPENGER_FORELDREPENGER("Feriepenger foreldrepenger", Kategori.YTELSE, FagsakYtelseType.FORELDREPENGER),
    FERIEPENGER_SVANGERSKAPSPENGER("Feriepenger svangerskapspenger", Kategori.YTELSE, FagsakYtelseType.SVANGERSKAPSPENGER),
    FERIEPENGER_OMSORGSPENGER("Feriepenger omsorgspenger", Kategori.YTELSE, FagsakYtelseType.OMSORGSPENGER),
    FERIEPENGER_OPPLÆRINGSPENGER("Feriepenger opplæringspenger", Kategori.YTELSE, FagsakYtelseType.OPPLÆRINGSPENGER),
    FERIEPENGER_PLEIEPENGER("Feriepenger pleiepenger", Kategori.YTELSE, FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
    FERIEPENGER_SYKEPENGER("Feriepenger sykepenger", Kategori.YTELSE, FagsakYtelseType.SYKEPENGER),
    FERIETILLEGG_DAGPENGER("Ferietillegg dagpenger ", Kategori.YTELSE, FagsakYtelseType.DAGPENGER),

    // Annen ytelse utbetalt til person
    KVALIFISERINGSSTØNAD("Kvalifiseringsstønad", Kategori.TRYGD, FagsakYtelseType.UDEFINERT),

    // Ytelse utbetalt til person som er næringsdrivende, fisker/lott, dagmamma eller jord/skogbruker
    FORELDREPENGER_NÆRING("Foreldrepenger næring", Kategori.NÆRING, FagsakYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER_NÆRING("Svangerskapspenger næring", Kategori.NÆRING, FagsakYtelseType.SVANGERSKAPSPENGER),
    SYKEPENGER_NÆRING("Sykepenger næring", Kategori.NÆRING, FagsakYtelseType.SYKEPENGER),
    OMSORGSPENGER_NÆRING("Omsorgspenger næring", Kategori.NÆRING, FagsakYtelseType.OMSORGSPENGER),
    OPPLÆRINGSPENGER_NÆRING("Opplæringspenger næring", Kategori.NÆRING, FagsakYtelseType.OPPLÆRINGSPENGER),
    PLEIEPENGER_NÆRING("Pleiepenger næring", Kategori.NÆRING, FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
    DAGPENGER_NÆRING("Dagpenger næring", Kategori.NÆRING, FagsakYtelseType.DAGPENGER),

    // Annen ytelse utbetalt til person som er næringsdrivende
    ANNET("Annet", Kategori.NÆRING, FagsakYtelseType.UDEFINERT),
    VEDERLAG("Vederlag", Kategori.NÆRING, FagsakYtelseType.UDEFINERT),
    LOTT_KUN_TRYGDEAVGIFT("Lott kun trygdeavgift", Kategori.NÆRING, FagsakYtelseType.UDEFINERT),
    KOMPENSASJON_FOR_TAPT_PERSONINNTEKT("Kompensasjon for tapt personinntekt", Kategori.NÆRING, FagsakYtelseType.FRISINN)
    ;

    public static final String KODEVERK = "INNTEKT_YTELSE_TYPE";

    private final String navn;
    private final FagsakYtelseType ytelseType;
    private final Kategori kategori;

    InntektYtelseType(String navn, Kategori kategori, FagsakYtelseType ytelseType) {
        this.navn = navn;
        this.kategori = kategori;
        this.ytelseType = ytelseType;
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

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    private boolean erOrdinærYtelse() {
        return kategori == Kategori.YTELSE;
    }

    private boolean erNæringsYtelse() {
        return kategori == Kategori.NÆRING;
    }

    public enum Kategori { YTELSE, NÆRING, TRYGD }
}
