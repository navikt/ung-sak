package no.nav.k9.sak.kontrakt.behandling;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.person.PersonDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

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

    @JsonProperty(value = "skalBehandlesAvInfotrygd")
    private Boolean skalBehandlesAvInfotrygd;

    @JsonProperty(value = "opprettet")
    private LocalDateTime opprettet;

    @JsonProperty(value = "endret")
    private LocalDateTime endret;

    @JsonProperty(value = "person", required = false)
    @Valid
    private PersonDto person;

    @JsonProperty(value = "pleietrengendeAktørId", required = false)
    @Valid
    private AktørId pleietrengendeAktørId;

    @JsonProperty(value = "relatertPersonAktørId", required = false)
    @Valid
    private AktørId relatertPersonAktørId;

    public FagsakDto() {
        // Injiseres i test
    }

    public FagsakDto(Saksnummer saksnummer,
                     FagsakYtelseType ytelseType,
                     FagsakStatus status,
                     Periode periode,
                     PersonDto person,
                     AktørId pleietrengendeAktørId,
                     AktørId relatertPersonAktørId,
                     Boolean kanRevurderingOpprettes,
                     Boolean skalBehandlesAvInfotrygd,
                     LocalDateTime opprettetTidspunkt,
                     LocalDateTime endretTidspunkt) {
        this.saksnummer = saksnummer;
        this.sakstype = ytelseType;
        this.status = status;
        this.gyldigPeriode = periode;
        this.person = person;
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.relatertPersonAktørId = relatertPersonAktørId;
        this.opprettet = opprettetTidspunkt;
        this.endret = endretTidspunkt;
        this.kanRevurderingOpprettes = kanRevurderingOpprettes;
        this.skalBehandlesAvInfotrygd = skalBehandlesAvInfotrygd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FagsakDto))
            return false;

        var other = (FagsakDto) o;

        if (!saksnummer.equals(other.saksnummer))
            return false;
        if (!sakstype.equals(other.sakstype))
            return false;
        if (!status.equals(other.status))
            return false;
        if (!person.equals(other.person))
            return false;
        if (opprettet != null ? !opprettet.equals(other.opprettet) : other.opprettet != null)
            return false;
        if (!Objects.equals(gyldigPeriode, other.gyldigPeriode))
            return false;

        return endret != null ? endret.equals(other.endret) : other.endret == null;
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

    public Boolean getSkalBehandlesAvInfotrygd() {
        return skalBehandlesAvInfotrygd;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        int result = saksnummer.hashCode();
        result = 31 * result + sakstype.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + person.hashCode();
        result = 31 * result + (opprettet != null ? opprettet.hashCode() : 0);
        result = 31 * result + (endret != null ? endret.hashCode() : 0);
        result = 31 * result + (gyldigPeriode != null ? gyldigPeriode.hashCode() : 0);
        return result;
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

    public void setSkalBehandlesAvInfotrygd(Boolean skalBehandlesAvInfotrygd) {
        this.skalBehandlesAvInfotrygd = skalBehandlesAvInfotrygd;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "<saksnummer=" + saksnummer +
            ", sakstype=" + sakstype +
            ", status=" + status +
            ", gyldigPeriode=" + gyldigPeriode +
            ", person=" + person +
            ", opprettet=" + opprettet +
            ", endret=" + endret +
            ">";
    }

}
