package no.nav.k9.sak.kontrakt.søknad.innsending;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(PleiepengerBarnSøknadInnsending.YTELSE_TYPE)
public class PleiepengerBarnSøknadInnsending extends SøknadInnsending<PleiepengerBarnSøknad> {

    public static final String YTELSE_TYPE = "PSB";
    
    @JsonAlias("payload")
    @JsonProperty(value = "søknad", required = true)
    @NotNull
    @Valid
    private PleiepengerBarnSøknad søknad;

    public PleiepengerBarnSøknadInnsending() {
        super(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
    }
    
    @JsonCreator
    public PleiepengerBarnSøknadInnsending(@JsonProperty(value = "saksnummer", required = true) @NotNull @Valid Saksnummer saksnummer,
                                           @JsonProperty(value = "søknad", required = true) @NotNull @Valid PleiepengerBarnSøknad søknad) {
        super(saksnummer, FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        this.søknad = søknad;
    }

    @Override
    public PleiepengerBarnSøknad getSøknad() {
        return søknad;
    }

    public void setSøknad(PleiepengerBarnSøknad søknad) {
        this.søknad = søknad;
    }
}
