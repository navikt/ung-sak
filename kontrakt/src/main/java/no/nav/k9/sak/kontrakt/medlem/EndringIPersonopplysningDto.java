package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class EndringIPersonopplysningDto {

    @JsonProperty(value = "endretAttributt")
    @Valid
    private EndringsresultatPersonopplysningerForMedlemskap.EndretAttributt endretAttributt;

    @JsonProperty(value = "erEndret")
    private boolean erEndret;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    LocalDate tom;

    public EndringIPersonopplysningDto() { // NOSONAR
        //
    }

    public EndringIPersonopplysningDto(EndringsresultatPersonopplysningerForMedlemskap.Endring endring) {
        erEndret = endring.isErEndret();
        endretAttributt = endring.getEndretAttributt();
        fom = endring.getPeriode().getFom();
        tom = endring.getPeriode().getTom();
    }

    public EndringsresultatPersonopplysningerForMedlemskap.EndretAttributt getEndretAttributt() {
        return endretAttributt;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean isErEndret() {
        return erEndret;
    }

    public void setEndretAttributt(EndringsresultatPersonopplysningerForMedlemskap.EndretAttributt endretAttributt) {
        this.endretAttributt = endretAttributt;
    }

    public void setErEndret(boolean erEndret) {
        this.erEndret = erEndret;
    }

    public void setFom(LocalDate fom) {
        this.fom = Objects.requireNonNull(fom, "fom");
    }

    public void setTom(LocalDate tom) {
        this.tom = Objects.requireNonNull(tom, "tom");
    }
}
