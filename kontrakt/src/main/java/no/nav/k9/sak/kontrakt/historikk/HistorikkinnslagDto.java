package no.nav.k9.sak.kontrakt.historikk;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagDto implements Comparable<HistorikkinnslagDto> {

    @JsonProperty(value = "aktoer")
    private HistorikkAktør aktoer;

    @JsonProperty(value = "behandlingId")
    private Long behandlingId;

    @JsonProperty(value = "dokumentLinks")
    private List<HistorikkInnslagDokumentLinkDto> dokumentLinks = Collections.emptyList();

    @JsonProperty(value = "historikkinnslagDeler")
    private List<HistorikkinnslagDelDto> historikkinnslagDeler = Collections.emptyList();

    @JsonProperty(value = "opprettetAv")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}ÆØÅæøå\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String opprettetAv;

    @JsonProperty(value = "opprettetTidspunkt")
    private LocalDateTime opprettetTidspunkt;

    @JsonProperty(value = "type")
    private HistorikkinnslagType type;

    public HistorikkinnslagDto() {
        //
    }

    @Override
    public int compareTo(HistorikkinnslagDto that) {
        int comparatorValue = that.getOpprettetTidspunkt().compareTo(this.getOpprettetTidspunkt());
        if (comparatorValue == 0 && that.getType().equals(HistorikkinnslagType.REVURD_OPPR)) {
            return -1;
        }
        return comparatorValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistorikkinnslagDto)) {
            return false;
        }
        HistorikkinnslagDto that = (HistorikkinnslagDto) o;
        return Objects.equals(getBehandlingId(), that.getBehandlingId()) &&
            Objects.equals(getType(), that.getType()) &&
            Objects.equals(getAktoer(), that.getAktoer()) &&
            Objects.equals(getOpprettetAv(), that.getOpprettetAv()) &&
            Objects.equals(getOpprettetTidspunkt(), that.getOpprettetTidspunkt()) &&
            Objects.equals(getDokumentLinks(), that.getDokumentLinks());
    }

    public HistorikkAktør getAktoer() {
        return aktoer;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public List<HistorikkInnslagDokumentLinkDto> getDokumentLinks() {
        return Collections.unmodifiableList(dokumentLinks);
    }

    public List<HistorikkinnslagDelDto> getHistorikkinnslagDeler() {
        return Collections.unmodifiableList(historikkinnslagDeler);
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public HistorikkinnslagType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBehandlingId(), getType(), getAktoer(), getOpprettetAv(), getOpprettetTidspunkt(), getDokumentLinks());
    }

    public void setAktoer(HistorikkAktør aktoer) {
        this.aktoer = aktoer;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setDokumentLinks(List<HistorikkInnslagDokumentLinkDto> dokumentLinks) {
        this.dokumentLinks = List.copyOf(dokumentLinks);
    }

    public void setHistorikkinnslagDeler(List<HistorikkinnslagDelDto> historikkinnslagDeler) {
        this.historikkinnslagDeler = List.copyOf(historikkinnslagDeler);
    }

    public void setOpprettetAv(String opprettetAv) {
        this.opprettetAv = opprettetAv;
    }

    public void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public void setType(HistorikkinnslagType type) {
        this.type = type;
    }
}
