package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Resultat;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingEndringDto {

    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    private UUID behandlingUuid;
    
    /**
     * IDen til SykdomVurdering (og ikke en gitt SykdomVurderingVersjon).
     */
    @JsonProperty(value = "id")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String id;
    
    /**
     * Versjonen man tok utgangspunkt i før endring.
     */
    @JsonProperty(value = "versjon")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String versjon;

    @JsonProperty(value = "tekst")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String tekst;

    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    @JsonProperty(value = "perioder")
    @Size(max = 100)
    @Valid
    private List<Periode> perioder = new ArrayList<>();

    @JsonProperty(value = "tilknyttedeDokumenter")
    @Size(max = 100)
    @Valid
    private Set<String> tilknyttedeDokumenter;
    
    @JsonProperty(value = "dryRun")
    @Valid
    private boolean dryRun;

    
    public SykdomVurderingEndringDto() {
        
    }
    
    public SykdomVurderingEndringDto(String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(behandlingUuid);
    }
    
    public SykdomVurderingEndringDto(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }
    
    
    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }
    
    public String getId() {
        return id;
    }

    public String getVersjon() {
        return versjon;
    }

    public String getTekst() {
        return tekst;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public List<Periode> getPerioder() {
        return perioder;
    }

    public Set<String> getTilknyttedeDokumenter() {
        return tilknyttedeDokumenter;
    }

    public boolean isDryRun() {
        return dryRun;
    }
    
    public SykdomVurderingEndringDto medId(String id) {
        this.id = id;
        return this;
    }
    
    public SykdomVurderingEndringDto medVersjon(String versjon) {
        this.versjon = versjon;
        return this;
    }

    public SykdomVurderingEndringDto medTekst(String tekst) {
        this.tekst = tekst;
        return this;
    }
    
    public SykdomVurderingEndringDto medResultat(Resultat resultat) {
        this.resultat = resultat;
        return this;
    }
    
    public SykdomVurderingEndringDto medPerioder(List<Periode> perioder) {
        this.perioder = perioder;
        return this;
    }

    public SykdomVurderingEndringDto medTilknyttedeDokumenter(Set<String> tilknyttedeDokumenter) {
        this.tilknyttedeDokumenter = tilknyttedeDokumenter;
        return this;
    }
}