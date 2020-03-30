package no.nav.k9.sak.kontrakt.søknad.innsending;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "ytelseType", defaultImpl = Void.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PleiepengerBarnSøknadInnsending.class, name = PleiepengerBarnSøknadInnsending.YTELSE_TYPE),
    @JsonSubTypes.Type(value = OmsorgspengerSøknadInnsending.class, name = OmsorgspengerSøknadInnsending.YTELSE_TYPE)
})
public abstract class SøknadInnsending<S> {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "ytelseType", required = true)
    @NotNull
    @Valid
    private FagsakYtelseType ytelseType;

    public SøknadInnsending(@NotNull @Valid Saksnummer saksnummer, @NotNull @Valid FagsakYtelseType ytelseType) {
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
        this.ytelseType = Objects.requireNonNull(ytelseType, "ytelseType");
    }

    protected SøknadInnsending(FagsakYtelseType ytelseType) {
        // kun for proxying
        this.ytelseType = ytelseType;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public abstract S getSøknad();
}
