package no.nav.k9.sak.kontrakt.kontroll;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.risikoklassifisering.FaresignalVurdering;
import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class KontrollresultatDto {

    @JsonProperty(value = "faresignalVurdering", required = true)
    @NotNull
    @Valid
    private FaresignalVurdering faresignalVurdering;

    @JsonProperty(value = "iayFaresignaler")
    @Valid
    private FaresignalgruppeDto iayFaresignaler;

    @JsonProperty(value = "kontrollresultat", required = true)
    @NotNull
    @Valid
    private Kontrollresultat kontrollresultat;

    @JsonProperty(value = "medlFaresignaler")
    @Valid
    private FaresignalgruppeDto medlFaresignaler;

    public KontrollresultatDto() {
        //
    }

    public static KontrollresultatDto ikkeKlassifisert() {
        KontrollresultatDto dto = new KontrollresultatDto();
        dto.setKontrollresultat(Kontrollresultat.IKKE_KLASSIFISERT);
        return dto;
    }

    public FaresignalVurdering getFaresignalVurdering() {
        return faresignalVurdering;
    }

    public FaresignalgruppeDto getIayFaresignaler() {
        return iayFaresignaler;
    }

    public Kontrollresultat getKontrollresultat() {
        return kontrollresultat;
    }

    public FaresignalgruppeDto getMedlFaresignaler() {
        return medlFaresignaler;
    }

    public void setFaresignalVurdering(FaresignalVurdering faresignalVurdering) {
        this.faresignalVurdering = faresignalVurdering;
    }

    public void setIayFaresignaler(FaresignalgruppeDto iayFaresignaler) {
        this.iayFaresignaler = iayFaresignaler;
    }

    public void setKontrollresultat(Kontrollresultat kontrollresultat) {
        this.kontrollresultat = kontrollresultat;
    }

    public void setMedlFaresignaler(FaresignalgruppeDto medlFaresignaler) {
        this.medlFaresignaler = medlFaresignaler;
    }
}
