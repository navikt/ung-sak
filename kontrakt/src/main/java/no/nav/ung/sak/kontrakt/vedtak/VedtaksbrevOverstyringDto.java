package no.nav.ung.sak.kontrakt.vedtak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.util.Collections;
import java.util.Set;

@Deprecated // Ikke i bruk
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public abstract class VedtaksbrevOverstyringDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "fritekstBrev")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String fritekstBrev;

    @JsonProperty(value = "overskrift")
    @Size(max = 200)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String overskrift;

    @JsonProperty(value = "skalBrukeOverstyrendeFritekstBrev", required = true)
    private boolean skalBrukeOverstyrendeFritekstBrev;

    @JsonProperty(value = "skalUndertrykkeBrev")
    private boolean skalUndertrykkeBrev = false;

    @JsonProperty(value = "redusertUtbetalingÅrsaker")
    @Size(max = 50)
    @Valid
    private Set<@NotNull @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String>
        redusertUtbetalingÅrsaker = Collections.emptySet();

    protected VedtaksbrevOverstyringDto() {
        // For Jackson
    }

    protected VedtaksbrevOverstyringDto(String begrunnelse, String overskrift, String fritekstBrev,
                                        boolean skalBrukeOverstyrendeFritekstBrev, Set<@NotNull String> redusertUtbetalingÅrsaker,
                                        boolean skalUndertrykkeBrev) {
        super(begrunnelse);
        this.overskrift = overskrift;
        this.fritekstBrev = fritekstBrev;
        this.skalBrukeOverstyrendeFritekstBrev = skalBrukeOverstyrendeFritekstBrev;
        this.setRedusertUtbetalingÅrsaker(redusertUtbetalingÅrsaker);
        this.skalUndertrykkeBrev = skalUndertrykkeBrev;
    }

    public String getFritekstBrev() {
        return fritekstBrev;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public boolean isSkalBrukeOverstyrendeFritekstBrev() {
        return skalBrukeOverstyrendeFritekstBrev;
    }

    public Set<String> getRedusertUtbetalingÅrsaker() {
        return redusertUtbetalingÅrsaker == null ? Collections.emptySet() : Collections.unmodifiableSet(redusertUtbetalingÅrsaker);
    }

    public void setFritekstBrev(String fritekstBrev) {
        this.fritekstBrev = fritekstBrev;
    }

    public void setOverskrift(String overskrift) {
        this.overskrift = overskrift;
    }

    public void setSkalBrukeOverstyrendeFritekstBrev(boolean skalBrukeOverstyrendeFritekstBrev) {
        this.skalBrukeOverstyrendeFritekstBrev = skalBrukeOverstyrendeFritekstBrev;
    }

    public void setRedusertUtbetalingÅrsaker(Set<String> redusertUtbetalingÅrsaker) {
        this.redusertUtbetalingÅrsaker = redusertUtbetalingÅrsaker == null ? Collections.emptySet() : Set.copyOf(redusertUtbetalingÅrsaker);
    }

    public boolean isSkalUndertrykkeBrev() {
        return skalUndertrykkeBrev;
    }

    public void setSkalUndertrykkeBrev(boolean skalUndertrykkeBrev) {
        this.skalUndertrykkeBrev = skalUndertrykkeBrev;
    }
}
