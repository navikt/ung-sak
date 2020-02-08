package no.nav.k9.sak.kontrakt.søknad;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class ManglendeVedleggDto {

    @JsonProperty(value = "dokumentType", required = true)
    @Valid
    private DokumentTypeId dokumentType;

    @JsonProperty(value = "arbeidsgiver")
    @Valid
    private ArbeidsgiverDto arbeidsgiver;

    @JsonProperty(value = "brukerHarSagtAtIkkeKommer")
    private boolean brukerHarSagtAtIkkeKommer;

    public ManglendeVedleggDto() {
        //
    }

    public DokumentTypeId getDokumentType() {
        return dokumentType;
    }

    public void setDokumentType(DokumentTypeId dokumentType) {
        this.dokumentType = dokumentType;
    }

    public ArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(ArbeidsgiverDto arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public boolean getBrukerHarSagtAtIkkeKommer() {
        return brukerHarSagtAtIkkeKommer;
    }

    public void setBrukerHarSagtAtIkkeKommer(boolean brukerHarSagtAtIkkeKommer) {
        this.brukerHarSagtAtIkkeKommer = brukerHarSagtAtIkkeKommer;
    }
}
