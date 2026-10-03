package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.hjemmel.AktivitetspengerForskrift;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public enum Avslagsårsak implements Kodeverdi {

    SØKT_FOR_SENT("1007", "Søkt for sent",
        Map.of(
            FagsakYtelseType.UNGDOMSYTELSE, "22-13, 3. ledd", // TODO: Legg til lovreferanse fra arbeidsmarkedsloven
            FagsakYtelseType.AKTIVITETSPENGER, "Folketrygdloven § 22-13")),
    MANGLENDE_DOKUMENTASJON("1019", "Manglende dokumentasjon",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "forskrift om ungdomsprogrammet § 4")),
    SØKER_UNDER_MINSTE_ALDER("1089", "Søker er yngre enn minste tillate alder.",
        Map.of(
            FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 3 bokstav a",
            FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("5"))),
    SØKER_OVER_HØYESTE_ALDER("1090", "Søker er eldre enn høyeste tillate alder.",
        Map.of(
            FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 3 bokstav a",
            FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("5"))),
    SØKER_HAR_AVGÅTT_MED_DØDEN("1091", "Søker har avgått med døden.",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "??")),

    OPPHØRT_UNGDOMSPROGRAM("2001", "Opphørt ungdomsprogram",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8")),
    ENDRET_STARTDATO_UNGDOMSPROGRAM("2002", "Endret start av ungdomsprogram",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8")),

    YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED("3001", "Søker har ikke bostedsadresse i Trondheim kommune.",
        Map.of(FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("2"))),

    SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE("3003", "Søker har annen livsoppholdsytelse.",
        Map.of(FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("4"))),

    YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE("3004", "Søker har ikke bostedsadresse i Trondheim kommune.",
        Map.of(FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("2"))),

    YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED("3005", "Søker har ikke bostedsadresse i Trondheim kommune.",
        Map.of(FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("2"))),

    IKKE_14A_VEDTAK("3002", "Søker har ikke oppfølgingsvedtak etter Navloven §14a.",
        Map.of(FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("6"))),

    //FIXME spesifikke avlagsårsaker for aktivitetsvilkåret er var ikke klare. Oppdater med faktiske årsaker når de er på plass
    AKTIVITETSVILKÅR_GENERELL_AVSLAGSÅRSAK("3011", "Søker oppfyller ikke aktivitetsvilkåret",
        Map.of(FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("8"))),

    SØKER_ER_IKKE_MEDLEM("4001", "Søker er ikke medlem.",
        Map.of(FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("3"))),



    /**
     * Denne årsaken brukes med noe som funksjonelt sett ikke er avslag - at saksbehandler velger å innvilge
     * en kortere periode enn det saksbehandlingssystemet tillater.
     */
    AVKORTET("9000", "Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.",
     Map.of(FagsakYtelseType.AKTIVITETSPENGER, AktivitetspengerForskrift.paragraf("10"))),

    UDEFINERT("-", "Ikke definert",
        Map.of());

    public static final String KODEVERK = "AVSLAGSARSAK"; //$NON-NLS-1$
    private static final Map<String, Avslagsårsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private Map<FagsakYtelseType, String> lovReferanser;

    private Avslagsårsak(String kode, String navn, Map<FagsakYtelseType, String> lovReferanser) {
        this.kode = kode;
        this.navn = navn;
        this.lovReferanser = lovReferanser;
    }

    public static Avslagsårsak fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Avslagsårsak: for input " + kode);
        }
        return ad;
    }

    public static Map<String, Avslagsårsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
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
    public String getOffisiellKode() {
        return getKode();
    }

    /**
     * Get vilkår dette avslaget kan opptre i.
     */
    public Set<VilkårType> getVilkårTyper() {
        return VilkårType.getVilkårTyper(this);
    }

    public String getLovHjemmelData(FagsakYtelseType ytelseType) {
        return lovReferanser.getOrDefault(ytelseType, "<mangler knytning lovhjemmel>");
    }

}
