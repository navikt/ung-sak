package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import no.nav.k9.abac.AbacAttributt;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OpprettForespørselRequest {
    @JsonProperty(value = "aktørId", required = true)
    @Valid
    @NotNull
    private String aktørId;

    @JsonProperty(value = "orgnummer", required = true)
    @Valid
    @NotNull
    private String orgnummer;

    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "ytelsetype", required = true)
    @Valid
    @NotNull
    private String ytelsetype;

    @JsonProperty(value = "saksnummer", required = true)
    @Valid
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String saksnummer;


    public OpprettForespørselRequest() {
    }

    public OpprettForespørselRequest(String aktørId,
                                     String orgnummer,
                                     LocalDate skjæringstidspunkt,
                                     String ytelsetype,
                                     String saksnummer) {
        this.aktørId = aktørId;
        this.orgnummer = orgnummer;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.ytelsetype = ytelsetype;
        this.saksnummer = saksnummer;
    }

    public String getAktørId() {
        return aktørId;
    }

    public String getOrgnummer() {
        return orgnummer;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public String getYtelsetype() {
        return ytelsetype;
    }

    public String getSaksnummer() {
        return saksnummer;
    }


}
