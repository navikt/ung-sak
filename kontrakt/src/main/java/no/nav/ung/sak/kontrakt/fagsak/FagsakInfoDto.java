package no.nav.ung.sak.kontrakt.fagsak;

import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;

@JsonInclude(value = Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class FagsakInfoDto {

    @JsonProperty(value = "saksnummer", required = true)
    @Valid
    @NotNull
    private Saksnummer saksnummer;

    @JsonProperty(value = "ytelseType", required = true)
    @Valid
    @Pattern(regexp = "^[a-zæøåA-ZÆØÅ0-9_]+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @NotNull
    private String ytelseType;

    @JsonProperty(value = "gyldigPeriode", required = false)
    @Valid
    private Periode gyldigPeriode;

    @JsonProperty(value = "status")
    @Valid
    @Pattern(regexp = "^[a-zæøåA-ZÆØÅ0-9_]+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String status;

    @JsonProperty(value = "person", required = false)
    @Valid
    private PersonIdent person;

    public FagsakInfoDto() {
        //
    }

    public FagsakInfoDto(Saksnummer saksnummer,
                         FagsakYtelseType ytelseType,
                         FagsakStatus status,
                         Periode periode,
                         PersonIdent person) {
        this.saksnummer = saksnummer;
        this.ytelseType = ytelseType.getKode();
        this.status = status == null ? null : status.getKode();
        this.gyldigPeriode = periode;
        this.person = person;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FagsakInfoDto))
            return false;

        var other = (FagsakInfoDto) o;

        return Objects.equals(saksnummer, other.saksnummer)
            && Objects.equals(ytelseType, other.ytelseType)
            && Objects.equals(status, other.status)
            && Objects.equals(person, other.person)
            && Objects.equals(gyldigPeriode, other.gyldigPeriode);
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return FagsakYtelseType.fraKode(ytelseType);
    }

    public Periode getGyldigPeriode() {
        return gyldigPeriode;
    }

    public FagsakStatus getStatus() {
        return FagsakStatus.fraKode(status);
    }

    public PersonIdent getPerson() {
        return person;
    }


    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, ytelseType, person, gyldigPeriode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<saksnummer=" + saksnummer +
            ", sakstype=" + ytelseType +
            ", status=" + status +
            ", gyldigPeriode=" + gyldigPeriode +
            ">";
    }

}
