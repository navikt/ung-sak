package no.nav.k9.sak.kontrakt.arbeidsforhold;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SøknadsperiodeOgOppgittOpptjeningDto {


    @JsonProperty(value = "førSøkerPerioden", required = true)
    @Valid
    @NotNull
    private OppgittOpptjeningDto førSøkerPerioden;
    @JsonProperty(value = "iSøkerPerioden", required = true)
    @Valid
    @NotNull
    private OppgittOpptjeningDto iSøkerPerioden;
    @JsonProperty(value = "periodeFraSøknad", required = true)
    @Valid
    @NotNull
    private PeriodeDto periodeFraSøknad;
    @JsonProperty(value = "søkerYtelseForFrilans", required = true)
    @Valid
    @NotNull
    private Boolean søkerYtelseForFrilans;
    @JsonProperty(value = "søkerYtelseForNæring", required = true)
    @Valid
    @NotNull
    private Boolean søkerYtelseForNæring;

    public SøknadsperiodeOgOppgittOpptjeningDto() {
        //jackson
    }

    public OppgittOpptjeningDto getFørSøkerPerioden() {
        return førSøkerPerioden;
    }

    public void setFørSøkerPerioden(OppgittOpptjeningDto førSøkerPerioden) {
        this.førSøkerPerioden = førSøkerPerioden;
    }

    public OppgittOpptjeningDto getISøkerPerioden() {
        return iSøkerPerioden;
    }

    public void setISøkerPerioden(OppgittOpptjeningDto iSøkerPerioden) {
        this.iSøkerPerioden = iSøkerPerioden;
    }

    public Boolean getSøkerYtelseForFrilans() {
        return søkerYtelseForFrilans;
    }

    public void setSøkerYtelseForFrilans(Boolean søkerYtelseForFrilans) {
        this.søkerYtelseForFrilans = søkerYtelseForFrilans;
    }

    public Boolean getSøkerYtelseForNæring() {
        return søkerYtelseForNæring;
    }

    public void setSøkerYtelseForNæring(Boolean søkerYtelseForNæring) {
        this.søkerYtelseForNæring = søkerYtelseForNæring;
    }

    public PeriodeDto getPeriodeFraSøknad() {
        return periodeFraSøknad;
    }

    public void setPeriodeFraSøknad(PeriodeDto periodeFraSøknad) {
        this.periodeFraSøknad = periodeFraSøknad;
    }
}

