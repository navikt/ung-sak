package no.nav.ung.sak.kontrakt.klage;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;

import java.util.Objects;

public class KlageVurderingResultatAksjonspunktMellomlagringDto {

    @JsonProperty(value = "kode")
    @Pattern(regexp = TekstValideringRegex.KODEVERK, message = "Kode [${validatedValue}] matcher ikke tillatt pattern '{value}'")
    private String kode;

    @JsonProperty(value = "behandlingId")
    @Min(0)
    @Max(Long.MAX_VALUE)
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?
    private Long behandlingId;

    @JsonProperty(value = "klageVurdering")
    private KlageVurderingType klageVurdering;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 100000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST)
    private String begrunnelse;

    @JsonProperty(value = "fritekstTilBrev")
    @Size(max = 100000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST)
    private String fritekstTilBrev;

    @JsonProperty(value = "klageHjemmel")
    @Size(max = 100)
    @Pattern(regexp = TekstValideringRegex.FRITEKST)
    private String hjemmel;

    @JsonProperty(value = "klageMedholdArsak")
    private KlageMedholdÅrsak klageMedholdArsak;

    @JsonProperty(value = "klageVurderingOmgjoer")
    private KlageVurderingOmgjør klageVurderingOmgjoer;

    public KlageVurderingResultatAksjonspunktMellomlagringDto() { // NOSONAR
        // For Jackson
    }

    public KlageVurderingResultatAksjonspunktMellomlagringDto( // NOSONAR
                                                              String kode,
                                                              Long behandlingId,
                                                              String begrunnelse,
                                                              KlageVurderingType klageVurderingType,
                                                              KlageMedholdÅrsak klageMedholdArsak,
                                                              String fritekstTilBrev,
                                                              String hjemmel,
                                                              KlageVurderingOmgjør klageVurderingOmgjoer) {
        this.kode = kode;
        this.behandlingId = behandlingId;
        this.begrunnelse = Objects.requireNonNull(begrunnelse, "begrunnelse");
        this.klageVurdering = klageVurderingType;
        this.begrunnelse = begrunnelse;
        this.fritekstTilBrev = fritekstTilBrev;
        this.hjemmel = hjemmel;
        this.klageMedholdArsak = klageMedholdArsak;
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
    }

    public KlageVurderingType getKlageVurdering() {
        return klageVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public String getHjemmel() {
        return hjemmel;
    }

    public KlageMedholdÅrsak getKlageMedholdArsak() {
        return klageMedholdArsak;
    }

    public KlageVurderingOmgjør getKlageVurderingOmgjoer() {
        return klageVurderingOmgjoer;
    }

    public String getKode() {
        return kode;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

}
