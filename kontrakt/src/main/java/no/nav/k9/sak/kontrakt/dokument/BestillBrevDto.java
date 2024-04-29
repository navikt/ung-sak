package no.nav.k9.sak.kontrakt.dokument;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.dokument.DokumentMalType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BestillBrevDto {

    @JsonProperty(value = "arsakskode")
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    public String arsakskode;

    @JsonProperty(value = "fritekst")
    @Size(max = 4000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    public String fritekst;

    @JsonProperty(value = "behandlingId", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @JsonProperty(value = "brevmalkode", required = true)
    @NotNull
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String brevmalkode;

    @JsonProperty(value = "overstyrtMottaker")
    @Valid
    private MottakerDto overstyrtMottaker;

    @JsonProperty(value = "fritekstbrev")
    @Valid
    private FritekstbrevinnholdDto fritekstbrev;

    /**
     * Kun et brev av med denne id'en blir bestilt - evt. påfølgende vil feile. Hvis null så skal random uuid brukes
     */
    @JsonProperty("dokumentbestillingsId")
    @Valid
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String dokumentbestillingsId;

    public BestillBrevDto() { // NOSONAR
    }

    public BestillBrevDto(long behandlingId, DokumentMalType dokumentMalType) {
        this(behandlingId, dokumentMalType, null, null, null, null);
    }

    public BestillBrevDto(long behandlingId, DokumentMalType dokumentMalType, String fritekst) {
        this(behandlingId, dokumentMalType, fritekst, null, null, null);
    }

    public BestillBrevDto(long behandlingId, DokumentMalType dokumentMalType, MottakerDto mottakerDto) {
        this(behandlingId, dokumentMalType, null, mottakerDto, null, null);
    }

    public BestillBrevDto(long behandlingId, DokumentMalType dokumentMalType, MottakerDto mottakerDto, String dokumentbestillingsId) {
        this(behandlingId, dokumentMalType, null, mottakerDto, null, dokumentbestillingsId);
    }


    public BestillBrevDto(long behandlingId, DokumentMalType dokumentMalType, String fritekst, MottakerDto overstyrtMottaker, FritekstbrevinnholdDto fritekstbrev, String dokumentbestillingsId) { // NOSONAR
        this.behandlingId = behandlingId;
        this.brevmalkode = dokumentMalType == null ? null : dokumentMalType.getKode();
        this.fritekst = fritekst;
        this.overstyrtMottaker = overstyrtMottaker;
        this.fritekstbrev = fritekstbrev;
        this.dokumentbestillingsId = dokumentbestillingsId;
    }

    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getBrevmalkode() {
        return brevmalkode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public MottakerDto getOverstyrtMottaker() {
        return overstyrtMottaker;
    }

    public FritekstbrevinnholdDto getFritekstbrev() {
        return fritekstbrev;
    }

    public void setArsakskode(String arsakskode) {
        this.arsakskode = arsakskode;
    }

    public void setÅrsakskode(String årsakskode) {
        this.arsakskode = årsakskode;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setBrevmalkode(String brevmalkode) {
        this.brevmalkode = brevmalkode;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }

    public void setOverstyrtMottaker(MottakerDto overstyrtMottaker) {
        this.overstyrtMottaker = overstyrtMottaker;
    }

    public void setFritekstbrev(FritekstbrevinnholdDto fritekstbrev) {
        this.fritekstbrev = fritekstbrev;
    }

    public String getDokumentbestillingsId() {
        return dokumentbestillingsId;
    }

    public void setDokumentbestillingsId(String dokumentbestillingsId) {
        this.dokumentbestillingsId = dokumentbestillingsId;
    }
}
