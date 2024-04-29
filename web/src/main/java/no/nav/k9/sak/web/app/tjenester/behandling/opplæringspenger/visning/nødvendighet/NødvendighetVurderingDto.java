package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.nødvendighet;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class NødvendighetVurderingDto {

    @JsonProperty(value = "journalpostId", required = true)
    @Valid
    @NotNull
    private JournalpostIdDto journalpostId;

    @JsonProperty(value = "perioder", required = true)
    @Size(min = 1)
    @Valid
    private List<Periode> perioder;

    @JsonProperty(value = "resultat", required = true)
    @Valid
    @NotNull
    private Resultat resultat;

    @JsonProperty(value = "begrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String begrunnelse;

    @JsonProperty(value = "vurdertAv", required = true)
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String vurdertAv;

    @JsonProperty(value = "vurdertTidspunkt", required = true)
    @Valid
    private LocalDateTime vurdertTidspunkt;

    @JsonProperty(value = "tilknyttedeDokumenter", required = true)
    @Size(max = 100)
    @Valid
    private List<String> tilknyttedeDokumenter;

    public NødvendighetVurderingDto(JournalpostIdDto journalpostId, List<Periode> perioder, Resultat resultat, String begrunnelse, String vurdertAv, LocalDateTime vurdertTidspunkt, List<String> tilknyttedeDokumenter) {
        this.journalpostId = journalpostId;
        this.perioder = perioder;
        this.resultat = resultat;
        this.begrunnelse = begrunnelse;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
        this.tilknyttedeDokumenter = tilknyttedeDokumenter;
    }

    public JournalpostIdDto getJournalpostId() {
        return journalpostId;
    }

    public List<Periode> getPerioder() {
        return perioder;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }

    public List<String> getTilknyttedeDokumenter() {
        return tilknyttedeDokumenter;
    }
}
