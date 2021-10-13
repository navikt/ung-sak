package no.nav.k9.kodeverk.arbeidsforhold;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum AktivitetStatus implements Kodeverdi {
    MIDLERTIDIG_INAKTIV("MIDL_INAKTIV", "Midlertidig inaktiv", Inntektskategori.UDEFINERT),
    ARBEIDSAVKLARINGSPENGER("AAP", "Arbeidsavklaringspenger", Inntektskategori.ARBEIDSAVKLARINGSPENGER),
    ARBEIDSTAKER("AT", "Arbeidstaker", Inntektskategori.ARBEIDSTAKER),
    DAGPENGER("DP", "Dagpenger", Inntektskategori.DAGPENGER),
    SYKEPENGER_AV_DAGPENGER("SP_AV_DP", "Sykepenger basert på dagpenger", Inntektskategori.DAGPENGER),
    FRILANSER("FL", "Frilanser", Inntektskategori.FRILANSER),
    MILITÆR_ELLER_SIVIL("MS", "Militær eller sivil", Inntektskategori.ARBEIDSTAKER),
    SELVSTENDIG_NÆRINGSDRIVENDE("SN", "Selvstendig næringsdrivende", Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE),
    KOMBINERT_AT_FL("AT_FL", "Kombinert arbeidstaker og frilanser", Inntektskategori.UDEFINERT),
    KOMBINERT_AT_SN("AT_SN", "Kombinert arbeidstaker og selvstendig næringsdrivende", Inntektskategori.UDEFINERT),
    KOMBINERT_FL_SN("FL_SN", "Kombinert frilanser og selvstendig næringsdrivende", Inntektskategori.UDEFINERT),
    KOMBINERT_AT_FL_SN("AT_FL_SN", "Kombinert arbeidstaker, frilanser og selvstendig næringsdrivende", Inntektskategori.UDEFINERT),
    BRUKERS_ANDEL("BA", "Brukers andel", Inntektskategori.UDEFINERT),
    IKKE_YRKESAKTIV("IKKE_YRKESAKTIV", "Ikke yrkesaktiv", Inntektskategori.UDEFINERT),
    KUN_YTELSE("KUN_YTELSE", "Kun ytelse", Inntektskategori.UDEFINERT),

    TTLSTØTENDE_YTELSE("TY", "Tilstøtende ytelse", Inntektskategori.UDEFINERT),
    VENTELØNN_VARTPENGER("VENTELØNN_VARTPENGER", "Ventelønn/Vartpenger", Inntektskategori.UDEFINERT),

    UDEFINERT("-", "Ikke definert", Inntektskategori.UDEFINERT);

    public static final String KODEVERK = "AKTIVITET_STATUS";
    @Deprecated
    public static final String DISCRIMINATOR = "AKTIVITET_STATUS";

    private static final Map<String, AktivitetStatus> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;

    @JsonIgnore
    private Inntektskategori inntektskategori;

    AktivitetStatus(String kode, String navn, Inntektskategori inntektskategori) {
        this.kode = kode;
        this.navn = navn;
        this.inntektskategori = inntektskategori;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static AktivitetStatus fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(AktivitetStatus.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AktivitetStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, AktivitetStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    private static final Set<AktivitetStatus> AT_STATUSER = Set.of(
        ARBEIDSTAKER, KOMBINERT_AT_FL_SN, KOMBINERT_AT_SN, KOMBINERT_AT_FL);

    private static final Set<AktivitetStatus> SN_STATUSER = Set.of(
        SELVSTENDIG_NÆRINGSDRIVENDE, KOMBINERT_AT_FL_SN, KOMBINERT_AT_SN, KOMBINERT_FL_SN);

    private static final Set<AktivitetStatus> FL_STATUSER = Set.of(
        FRILANSER, KOMBINERT_AT_FL_SN, KOMBINERT_AT_FL, KOMBINERT_FL_SN);

    public boolean erArbeidstaker() {
        return AT_STATUSER.contains(this);
    }

    public boolean erSelvstendigNæringsdrivende() {
        return SN_STATUSER.contains(this);
    }

    public boolean erFrilanser() {
        return FL_STATUSER.contains(this);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(value = "kodeverk", access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty(value = "kode")
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }
}
