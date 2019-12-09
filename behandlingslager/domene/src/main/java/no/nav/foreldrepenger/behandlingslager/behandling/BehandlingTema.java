package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingTema implements Kodeverdi {
    PLEIEPENGER_SYKT_BARN("PLEIE", "Pleiepenger sykt barn", "ab0320"),  // ny ordning fom 011017

    // FIXME K9 - kodeverk for k9 ytelser i stedet
    ENGANGSSTØNAD("ENGST", "Engangsstønad", "ab0327", FagsakYtelseType.ENGANGSTØNAD),
    ENGANGSSTØNAD_FØDSEL("ENGST_FODS", "Engangsstønad ved fødsel", "ab0050", FagsakYtelseType.ENGANGSTØNAD),
    ENGANGSSTØNAD_ADOPSJON("ENGST_ADOP", "Engangsstønad ved adopsjon", "ab0027", FagsakYtelseType.ENGANGSTØNAD),
    FORELDREPENGER("FORP", "Foreldrepenger", "ab0326", FagsakYtelseType.FORELDREPENGER),
    FORELDREPENGER_ADOPSJON("FORP_ADOP", "Foreldrepenger ved adopsjon", "ab0072", FagsakYtelseType.FORELDREPENGER),
    FORELDREPENGER_FØDSEL("FORP_FODS", "Foreldrepenger ved fødsel", "ab0047", FagsakYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger", "ab0126", FagsakYtelseType.SVANGERSKAPSPENGER),
    UDEFINERT("-", "Ikke definert", null, FagsakYtelseType.UDEFINERT),

    ;

    private static final Map<String, BehandlingTema> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_TEMA";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private String kode;

    private FagsakYtelseType fagsakYtelseType;

    private BehandlingTema(String kode) {
        this.kode = kode;
    }

    private BehandlingTema(String kode, String navn, String offisiellKode, FagsakYtelseType fagsakYtelseType) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
        this.fagsakYtelseType = fagsakYtelseType;
    }


    @JsonCreator
    public static BehandlingTema fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingTema: " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingTema> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(k -> "'" + k + "'").collect(Collectors.toList()));
    }

    public static BehandlingTema finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

    public static boolean gjelderEngangsstønad(BehandlingTema behandlingTema) {
        return ENGANGSSTØNAD_ADOPSJON.equals(behandlingTema) || ENGANGSSTØNAD_FØDSEL.equals(behandlingTema) || ENGANGSSTØNAD.equals(behandlingTema);
    }

    public static boolean gjelderForeldrepenger(BehandlingTema behandlingTema) {
        return FORELDREPENGER_ADOPSJON.equals(behandlingTema) || FORELDREPENGER_FØDSEL.equals(behandlingTema) || FORELDREPENGER.equals(behandlingTema);
    }

    public static boolean gjelderSvangerskapspenger(BehandlingTema behandlingTema) {
        return SVANGERSKAPSPENGER.equals(behandlingTema);
    }

    public static boolean ikkeSpesifikkHendelse(BehandlingTema behandlingTema) {
        return FORELDREPENGER.equals(behandlingTema) || ENGANGSSTØNAD.equals(behandlingTema) || UDEFINERT.equals(behandlingTema);
    }

    public static boolean gjelderSammeYtelse(BehandlingTema tema1, BehandlingTema tema2) {
        return (gjelderForeldrepenger(tema1) && gjelderForeldrepenger(tema2))
            || (gjelderEngangsstønad(tema1) && gjelderEngangsstønad(tema2))
            || (gjelderSvangerskapspenger(tema1) && gjelderSvangerskapspenger(tema2));

    }

    /**
     * Returnerer true hvis angitt tema gjelder samme ytelse og hendelse som denne. Hendlse trenger kun matche hvis begge temaene amgir dette
     * eksplisitt.
     */
    public boolean erKompatibelMed(BehandlingTema that) {
        return gjelderSammeYtelse(this, that) && (ikkeSpesifikkHendelse(this) || ikkeSpesifikkHendelse(that) || equals(that));
    }

    public static BehandlingTema fraFagsak(Fagsak fagsak) {
     // FIXME K9 kodeverk/logikk
        return fraFagsakHendelse(fagsak.getYtelseType());
    }

    public static BehandlingTema fraFagsakHendelse(FagsakYtelseType ytelseType) {
        // FIXME K9 kodeverk/logikk
        return BehandlingTema.UDEFINERT;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }
}
