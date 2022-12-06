package no.nav.k9.kodeverk.uttak;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum UttakArbeidType implements Kodeverdi {

    ARBEIDSTAKER(AktivitetStatus.ARBEIDSTAKER, "Ordinært arbeid"),
    SELVSTENDIG_NÆRINGSDRIVENDE(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, "Selvstendig næringsdrivende"),
    FRILANSER(AktivitetStatus.FRILANSER, "Frilans"),

    DAGPENGER(AktivitetStatus.DAGPENGER, "Dagpenger"),
    SYKEPENGER_AV_DAGPENGER(AktivitetStatus.SYKEPENGER_AV_DAGPENGER, "Sykepenger basert på dagpenger"),
    PLEIEPENGER_AV_DAGPENGER(AktivitetStatus.PLEIEPENGER_AV_DAGPENGER, "Pleiepenger basert på dagpenger"),
    KUN_YTELSE(AktivitetStatus.BRUKERS_ANDEL, "Kun ytelse"),

    IKKE_YRKESAKTIV(AktivitetStatus.IKKE_YRKESAKTIV, "Ikke yrkesaktiv"),
    IKKE_YRKESAKTIV_UTEN_ERSTATNING("IKKE_YRKESAKTIV_UTEN_ERSTATNING", "Ikke yrkesaktiv som ikke har blitt erstattet med nytt"),

    // 8-47 opptjening gir inaktiv
    INAKTIV(AktivitetStatus.MIDLERTIDIG_INAKTIV, "Inaktiv"),
    ANNET("ANNET", "Annet"),
    ;

    public static final EnumSet<UttakArbeidType> ATFL = EnumSet.of(ARBEIDSTAKER, FRILANSER);
    public static final String KODEVERK = "UTTAK_ARBEID_TYPE";
    private static final Map<String, UttakArbeidType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            KODER.putIfAbsent(v.name(), v); // fallback for Jackson enum key i map issue (løses delvis i jackson 2.11)
        }
    }

    @JsonIgnore
    private String navn;

    @JsonValue
    private String kode;

    @JsonIgnore
    private AktivitetStatus aktivitetStatus;

    UttakArbeidType(AktivitetStatus aktivitetStatus, String navn) {
        this.aktivitetStatus = aktivitetStatus;
        this.kode = aktivitetStatus.getKode();
        this.navn = navn;
    }

    UttakArbeidType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static UttakArbeidType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UttakArbeidType: " + kode);
        }
        return ad;
    }

    public static Map<String, UttakArbeidType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static UttakArbeidType mapFra(AktivitetStatus aktivitetStatus) {
        for (var ut : values()) {
            if (ut.aktivitetStatus != null && ut.aktivitetStatus.equals(aktivitetStatus)) {
                return ut;
            }
        }
        if (Inntektskategori.UDEFINERT.equals(aktivitetStatus.getInntektskategori())) {
            throw new IllegalArgumentException(AktivitetStatus.class.getSimpleName() + "AktivitetStatus " + aktivitetStatus + " mangler mapping til " + UttakArbeidType.class.getSimpleName());
        } else {
            return UttakArbeidType.ANNET;
        }
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
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return this.getKode();
    }

    public boolean erArbeidstakerEllerFrilans() {
        return ARBEIDSTAKER.equals(this) || FRILANSER.equals(this);
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public boolean matcher(AktivitetStatus aktivitetStatus) {
        if (UttakArbeidType.IKKE_YRKESAKTIV.equals(this) || UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING.equals(this)) {
            return Objects.equals(UttakArbeidType.ARBEIDSTAKER.getAktivitetStatus().getKode(), aktivitetStatus.getKode());
        }
        if (UttakArbeidType.INAKTIV.equals(this)) {
            return Objects.equals(UttakArbeidType.INAKTIV.getAktivitetStatus().getKode(), aktivitetStatus.getKode());
        }
        return Objects.equals(this.getAktivitetStatus().getKode(), aktivitetStatus.getKode());
    }

}
