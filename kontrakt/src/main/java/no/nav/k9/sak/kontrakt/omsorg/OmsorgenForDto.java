package no.nav.k9.sak.kontrakt.omsorg;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.typer.Periode;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OmsorgenForDto {

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    @JsonProperty(value = "relasjon")
    @Valid
    private BarnRelasjon relasjon;

    @JsonProperty(value = "relasjonsbeskrivelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String relasjonsbeskrivelse;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String begrunnelse;

    @JsonProperty(value = "readOnly")
    @Valid
    private boolean readOnly;

    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    @JsonProperty(value = "resultatEtterAutomatikk")
    @Valid
    private Resultat resultatEtterAutomatikk;


    OmsorgenForDto() {

    }

    public OmsorgenForDto(Periode periode, String begrunnelse, BarnRelasjon relasjon, String relasjonsbeskrivelse, boolean readOnly, Resultat resultat, Resultat resultatEtterAutomatikk) {
        this.periode = periode;
        this.begrunnelse = begrunnelse;
        this.relasjon = relasjon;
        this.relasjonsbeskrivelse = relasjonsbeskrivelse;
        this.readOnly = readOnly;
        this.resultat = resultat;
        this.resultatEtterAutomatikk = resultatEtterAutomatikk;
    }


    public Periode getPeriode() {
        return periode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public BarnRelasjon getRelasjon() {
        return relasjon;
    }

    public String getRelasjonsbeskrivelse() {
        return relasjonsbeskrivelse;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public Resultat getResultatEtterAutomatikk() {
        return resultatEtterAutomatikk;
    }
}
