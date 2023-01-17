package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.reisetid;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReisetidPeriodeVurderingDto {

    @JsonProperty(value = "periode", required = true)
    @Valid
    private Periode periode;

    @JsonProperty(value = "resultat", required = true)
    @Valid
    @NotNull
    private Resultat resultat;

    @JsonProperty(value = "begrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String begrunnelse;

    @JsonProperty(value = "endretAv", required = true)
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String endretAv;

    @JsonProperty(value = "endretTidspunkt", required = true)
    @Valid
    private LocalDateTime endretTidspunkt;

    public ReisetidPeriodeVurderingDto(Periode periode, Resultat resultat, String begrunnelse, String endretAv, LocalDateTime endretTidspunkt) {
        this.periode = periode;
        this.resultat = resultat;
        this.begrunnelse = begrunnelse;
        this.endretAv = endretAv;
        this.endretTidspunkt = endretTidspunkt;
    }

    public ReisetidPeriodeVurderingDto(LocalDate fom, LocalDate tom, Resultat resultat, String begrunnelse, String endretAv, LocalDateTime endretTidspunkt) {
        this(new Periode(fom, tom), resultat, begrunnelse, endretAv, endretTidspunkt);
    }

    public Periode getPeriode() {
        return periode;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }
}
