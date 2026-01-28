package no.nav.ung.sak.kontrakt.fagsak;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.kontrakt.person.PersonDto;
import no.nav.ung.sak.felles.typer.Periode;
import no.nav.ung.sak.felles.typer.Saksnummer;

import java.time.LocalDateTime;
import java.util.Objects;

@JsonInclude(value = Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class FagsakDto {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    private Saksnummer saksnummer;

    @JsonProperty(value = "sakstype", required = true)
    @Valid
    @NotNull
    private FagsakYtelseType sakstype;

    @JsonProperty(value = "gyldigPeriode", required = false)
    @Valid
    private Periode gyldigPeriode;

    @JsonProperty(value = "status")
    @Valid
    private FagsakStatus status;

    @JsonProperty(value = "kanRevurderingOpprettes")
    private Boolean kanRevurderingOpprettes;

    @JsonProperty(value = "opprettet")
    private LocalDateTime opprettet;

    @JsonProperty(value = "endret")
    private LocalDateTime endret;

    @JsonProperty(value = "person", required = false)
    @Valid
    private PersonDto person;

    @JsonProperty(value = "erIkkeDigitalBruker")
    private Boolean erIkkeDigitalBruker;

    public FagsakDto() {
        // Injiseres i test
    }

    public FagsakDto(Saksnummer saksnummer,
                     FagsakYtelseType ytelseType,
                     FagsakStatus status,
                     Periode periode,
                     PersonDto person,
                     Boolean kanRevurderingOpprettes,
                     LocalDateTime opprettetTidspunkt,
                     LocalDateTime endretTidspunkt,
                     Boolean erIkkeDigitalBruker) {
        this.saksnummer = saksnummer;
        this.sakstype = ytelseType;
        this.status = status;
        this.gyldigPeriode = periode;
        this.person = person;
        this.opprettet = opprettetTidspunkt;
        this.endret = endretTidspunkt;
        this.kanRevurderingOpprettes = kanRevurderingOpprettes;
        this.erIkkeDigitalBruker = erIkkeDigitalBruker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FagsakDto))
            return false;

        var other = (FagsakDto) o;
        return Objects.equals(saksnummer, other.saksnummer)
            && Objects.equals(sakstype, other.sakstype)
            && Objects.equals(status, other.status)
            && Objects.equals(person, other.person)
            && Objects.equals(gyldigPeriode, other.gyldigPeriode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, sakstype, status, person, gyldigPeriode);
    }

    public LocalDateTime getEndret() {
        return endret;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public PersonDto getPerson() {
        return person;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getSakstype() {
        return sakstype;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setEndret(LocalDateTime endret) {
        this.endret = endret;
    }

    public void setOpprettet(LocalDateTime opprettet) {
        this.opprettet = opprettet;
    }

    public void setPerson(PersonDto person) {
        this.person = person;
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public void setSakstype(FagsakYtelseType sakstype) {
        this.sakstype = sakstype;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<saksnummer=" + saksnummer +
            ", sakstype=" + sakstype +
            ", status=" + status +
            ", gyldigPeriode=" + gyldigPeriode +
            ", person=" + person +
            ", opprettet=" + opprettet +
            ", endret=" + endret +
            ">";
    }

}
