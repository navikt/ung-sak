package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.sak.typer.Saksnummer;

import java.time.LocalDate;
import java.util.Objects;

final class HarGyldigOmsorgsdagerVedtakDto extends BrukerdialogEvaluation {
    @JsonProperty private final Boolean harInnvilgedeBehandlinger;
    @JsonProperty private final @Valid Saksnummer saksnummer;
    @JsonProperty private final LocalDate vedtaksdato;

    HarGyldigOmsorgsdagerVedtakDto(
            Boolean harInnvilgedeBehandlinger,
            @Valid Saksnummer saksnummer,
            LocalDate vedtaksdato,
            Evaluation evaluation
    ) {
        super(evaluation);
        this.harInnvilgedeBehandlinger = harInnvilgedeBehandlinger;
        this.saksnummer = saksnummer;
        this.vedtaksdato = vedtaksdato;
    }

    public Boolean harInnvilgedeBehandlinger() {
        return harInnvilgedeBehandlinger;
    }

    public @Valid Saksnummer saksnummer() {
        return saksnummer;
    }

    public LocalDate vedtaksdato() {
        return vedtaksdato;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (HarGyldigOmsorgsdagerVedtakDto) obj;
        return Objects.equals(this.harInnvilgedeBehandlinger, that.harInnvilgedeBehandlinger) &&
                Objects.equals(this.saksnummer, that.saksnummer) &&
                Objects.equals(this.vedtaksdato, that.vedtaksdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(harInnvilgedeBehandlinger, saksnummer, vedtaksdato);
    }

    @Override
    public String toString() {
        return "HarGyldigOmsorgsdagerVedtakDto[" +
                "harInnvilgedeBehandlinger=" + harInnvilgedeBehandlinger + ", " +
                "saksnummer=" + saksnummer + ", " +
                "vedtaksdato=" + vedtaksdato + ']';
    }

}
