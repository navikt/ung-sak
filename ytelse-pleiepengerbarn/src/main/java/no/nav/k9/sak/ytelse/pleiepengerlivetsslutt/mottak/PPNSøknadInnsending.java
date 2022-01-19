package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.søknad.Søknad;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(PPNSøknadInnsending.YTELSE_TYPE)
public class PPNSøknadInnsending extends InnsendingInnhold {

    public static final String YTELSE_TYPE = "PPN";

    @JsonAlias("payload")
    @JsonProperty(value = "søknad", required = true)
    @NotNull
    @Valid
    private Søknad søknad;

    public PPNSøknadInnsending() {
        super(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE);
    }

    @JsonCreator
    public PPNSøknadInnsending(@JsonProperty(value = "søknad", required = true) @NotNull @Valid Søknad søknad) {
        this();
        this.søknad = søknad;
    }

    public Søknad getSøknad() {
        return søknad;
    }

    public void setSøknad(Søknad søknad) {
        this.søknad = søknad;
    }
}
