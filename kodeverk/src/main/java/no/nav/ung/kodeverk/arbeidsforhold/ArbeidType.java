package no.nav.ung.kodeverk.arbeidsforhold;

/**
 * Typer av arbeidsforhold.
 * <p>
 * <h3>Kilde: NAV kodeverk</h3>
 * https://modapp.adeo.no/kodeverksklient/viskodeverk/Arbeidsforholdstyper/2
 * <p>
 * <h3>Tjeneste(r) som returnerer dette:</h3>
 * <ul>
 * <li>https://confluence.adeo.no/display/SDFS/tjeneste_v3%3Avirksomhet%3AArbeidsforhold_v3</li>
 * </ul>
 * <h3>Tjeneste(r) som konsumerer dete:</h3>
 * <ul>
 * <li></li>
 * </ul>
 */

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

public enum ArbeidType implements Kodeverdi {

    ETTERLØNN_SLUTTPAKKE("ETTERLØNN_SLUTTPAKKE", "Etterlønn eller sluttpakke", null, true),
    FORENKLET_OPPGJØRSORDNING("FORENKLET_OPPGJØRSORDNING", "Forenklet oppgjørsordning ", "forenkletOppgjoersordning", false),
    FRILANSER("FRILANSER", "Frilanser, samlet aktivitet", null, true),
    FRILANSER_OPPDRAGSTAKER_MED_MER("FRILANSER_OPPDRAGSTAKER", "Frilansere/oppdragstakere, med mer", "frilanserOppdragstakerHonorarPersonerMm", false),
    LØNN_UNDER_UTDANNING("LØNN_UNDER_UTDANNING", "Lønn under utdanning", null, true),
    MARITIMT_ARBEIDSFORHOLD("MARITIMT_ARBEIDSFORHOLD", "Maritimt arbeidsforhold", "maritimtArbeidsforhold", false),
    MILITÆR_ELLER_SIVILTJENESTE("MILITÆR_ELLER_SIVILTJENESTE", "Militær eller siviltjeneste", null, true),
    ORDINÆRT_ARBEIDSFORHOLD("ORDINÆRT_ARBEIDSFORHOLD", "Ordinært arbeidsforhold", "ordinaertArbeidsforhold", false),
    PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD("PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD", "Pensjoner og andre typer ytelser",
            "pensjonOgAndreTyperYtelserUtenAnsettelsesforhold", false),
    SELVSTENDIG_NÆRINGSDRIVENDE("NÆRING", "Selvstendig næringsdrivende", null, false),
    UTENLANDSK_ARBEIDSFORHOLD("UTENLANDSK_ARBEIDSFORHOLD", "Arbeid i utlandet", null, true),
    VENTELØNN_VARTPENGER("VENTELØNN_VARTPENGER", "Ventelønn eller vartpenger", null, true),
    VANLIG("VANLIG", "Vanlig", "VANLIG", false),
    UDEFINERT("-", "Ikke definert", null, false),
    ;

    public static final Set<ArbeidType> AA_REGISTER_TYPER = Set.of(
        ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
        ArbeidType.MARITIMT_ARBEIDSFORHOLD,
        ArbeidType.FORENKLET_OPPGJØRSORDNING);

    public static final String KODEVERK = "ARBEID_TYPE";

    private static final Map<String, ArbeidType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;

    private String navn;

    private String offisiellKode;

    private boolean visGui;

    private ArbeidType(String kode) {
        this.kode = kode;
    }

    private ArbeidType(String kode, String navn, String offisiellKode, boolean visGui) {
        this.kode = kode;
        this.navn = navn;
        this.visGui = visGui;
        this.offisiellKode = offisiellKode;
    }

    public static ArbeidType finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

    public static ArbeidType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ArbeidType: " + kode);
        }
        return ad;
    }

    public static Map<String, ArbeidType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public boolean erAnnenOpptjening() {
        return visGui;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
