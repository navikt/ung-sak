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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingOpprettelseDto {

    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    private UUID behandlingUuid;
    
    @JsonProperty(value = "type", required = true)
    @NotNull
    @Valid
    private SykdomVurderingType type;

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

    public SykdomVurderingOpprettelseDto() {
     
    }
    
    public SykdomVurderingOpprettelseDto(String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(behandlingUuid);
    }
    
    public SykdomVurderingOpprettelseDto(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }
    
    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public SykdomVurderingType getType() {
        return type;
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
    
    public SykdomVurderingOpprettelseDto medTekst(String tekst) {
        this.tekst = tekst;
        return this;
    }
    
    public SykdomVurderingOpprettelseDto medResultat(Resultat resultat) {
        this.resultat = resultat;
        return this;
    }
    
    public SykdomVurderingOpprettelseDto medPerioder(List<Periode> perioder) {
        this.perioder = perioder;
        return this;
    }

    public SykdomVurderingOpprettelseDto medTilknyttedeDokumenter(Set<String> tilknyttedeDokumenter) {
        this.tilknyttedeDokumenter = tilknyttedeDokumenter;
        return this;
    }
}