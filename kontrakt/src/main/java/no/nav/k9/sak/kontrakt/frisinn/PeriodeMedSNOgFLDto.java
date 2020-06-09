package no.nav.k9.sak.kontrakt.frisinn;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PeriodeMedSNOgFLDto {


    @JsonProperty(value = "måned", required = true)
    @Valid
    @NotNull
    private PeriodeDto måned;

    @JsonProperty(value = "oppgittIMåned", required = true)
    @Valid
    @NotNull
    private OppgittOpptjeningDto oppgittIMåned;

    @JsonProperty(value = "søkerFL", required = true)
    @Valid
    @NotNull
    private Boolean søkerFL;

    @JsonProperty(value = "søkerSN", required = true)
    @Valid
    @NotNull
    private Boolean søkerSN;

    public PeriodeMedSNOgFLDto() {
        //jackson
    }

    public OppgittOpptjeningDto getOppgittIMåned() {
        return oppgittIMåned;
    }

    public void setOppgittIMåned(OppgittOpptjeningDto iSøkerPerioden) {
        this.oppgittIMåned = iSøkerPerioden;
    }

    public Boolean getSøkerFL() {
        return søkerFL;
    }

    public void setSøkerFL(Boolean søkerFL) {
        this.søkerFL = søkerFL;
    }

    public Boolean getSøkerSN() {
        return søkerSN;
    }

    public void setSøkerSN(Boolean søkerSN) {
        this.søkerSN = søkerSN;
    }

    public PeriodeDto getMåned() {
        return måned;
    }

    public void setMåned(PeriodeDto måned) {
        this.måned = måned;
    }
}

