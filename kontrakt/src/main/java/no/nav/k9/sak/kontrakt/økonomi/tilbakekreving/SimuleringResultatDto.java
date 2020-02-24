package no.nav.k9.sak.kontrakt.økonomi.tilbakekreving;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimuleringResultatDto {

    private boolean slåttAvInntrekk;
    private Long sumFeilutbetaling;
    private Long sumInntrekk;

    public SimuleringResultatDto() {
    }

    public SimuleringResultatDto(Long sumFeilutbetaling, Long sumInntrekk, boolean slåttAvInntrekk) {
        this.sumFeilutbetaling = sumFeilutbetaling;
        this.sumInntrekk = sumInntrekk;
        this.slåttAvInntrekk = slåttAvInntrekk;
    }

    public Long getSumFeilutbetaling() {
        return sumFeilutbetaling;
    }

    public Long getSumInntrekk() {
        return sumInntrekk;
    }

    public boolean isSlåttAvInntrekk() {
        return slåttAvInntrekk;
    }
}
