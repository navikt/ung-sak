package no.nav.k9.sak.kontrakt.behandling;

import java.time.LocalDateTime;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.person.PersonDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class FagsakDto {

    @JsonProperty(value = "endret")
    private LocalDateTime endret;

    @JsonProperty(value = "kanRevurderingOpprettes")
    private Boolean kanRevurderingOpprettes;

    @JsonProperty(value = "opprettet", required = true)
    @NotNull
    private LocalDateTime opprettet;

    @JsonProperty(value = "person", required = true)
    @NotNull
    @Valid
    private PersonDto person;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    private Saksnummer saksnummer;

    @JsonProperty(value = "sakstype", required = true)
    @NotNull
    private FagsakYtelseType sakstype;

    @JsonProperty(value = "skalBehandlesAvInfotrygd")
    private Boolean skalBehandlesAvInfotrygd;

    @JsonProperty(value = "status", required = true)
    @NotNull
    private FagsakStatus status;

    @JsonProperty(value = "aktørId")
    @NotNull
    private AktørId aktørId;

    public FagsakDto() {
        // Injiseres i test
    }

    public FagsakDto(Saksnummer saksnummer,
                     FagsakYtelseType ytelseType,
                     FagsakStatus status,
                     PersonDto person,
                     Boolean kanRevurderingOpprettes,
                     Boolean skalBehandlesAvInfotrygd,
                     LocalDateTime opprettetTidspunkt,
                     LocalDateTime endretTidspunkt,
                     AktørId aktørId) {
        this.saksnummer = saksnummer;
        this.sakstype = ytelseType;
        this.status = status;
        this.person = person;
        this.opprettet = opprettetTidspunkt;
        this.endret = endretTidspunkt;
        this.kanRevurderingOpprettes = kanRevurderingOpprettes;
        this.skalBehandlesAvInfotrygd = skalBehandlesAvInfotrygd;
        this.aktørId = aktørId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FagsakDto))
            return false;

        FagsakDto fagsakDto = (FagsakDto) o;

        if (!saksnummer.equals(fagsakDto.saksnummer))
            return false;
        if (!sakstype.equals(fagsakDto.sakstype))
            return false;
        if (!status.equals(fagsakDto.status))
            return false;
        if (!person.equals(fagsakDto.person))
            return false;
        if (opprettet != null ? !opprettet.equals(fagsakDto.opprettet) : fagsakDto.opprettet != null)
            return false;
        if (!aktørId.equals(fagsakDto.aktørId))
            return false;
        return endret != null ? endret.equals(fagsakDto.endret) : fagsakDto.endret == null;
    }

    public LocalDateTime getEndret() {
        return endret;
    }

    public Boolean getKanRevurderingOpprettes() {
        return kanRevurderingOpprettes;
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

    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public int hashCode() {
        int result = saksnummer.hashCode();
        result = 31 * result + sakstype.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + person.hashCode();
        result = 31 * result + (opprettet != null ? opprettet.hashCode() : 0);
        result = 31 * result + (endret != null ? endret.hashCode() : 0);
        return result;
    }

    public void setEndret(LocalDateTime endret) {
        this.endret = endret;
    }

    public void setKanRevurderingOpprettes(Boolean kanRevurderingOpprettes) {
        this.kanRevurderingOpprettes = kanRevurderingOpprettes;
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

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    @Override
    public String toString() {
        return "<saksnummer=" + saksnummer + //$NON-NLS-1$
            ", sakstype=" + sakstype + //$NON-NLS-1$
            ", status=" + status + //$NON-NLS-1$
            ", person=" + person + //$NON-NLS-1$
            ", opprettet=" + opprettet + //$NON-NLS-1$
            ", endret=" + endret + //$NON-NLS-1$
            ">";
    }

}
