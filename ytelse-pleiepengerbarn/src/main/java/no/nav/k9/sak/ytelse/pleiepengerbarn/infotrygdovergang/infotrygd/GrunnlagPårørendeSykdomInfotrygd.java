package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import no.nav.k9.sak.typer.Periode;

@JsonDeserialize(builder = GrunnlagPårørendeSykdomInfotrygd.Builder.class)
class GrunnlagPårørendeSykdomInfotrygd {

    private Kodeverdi tema;
    private Periode periode;
    private Kodeverdi behandlingstema;
    private LocalDate opphoerFom;
    private List<VedtakPårørendeSykdomInfotrygd> vedtak;

    public GrunnlagPårørendeSykdomInfotrygd(Kodeverdi tema,
                                            Periode periode,
                                            Kodeverdi behandlingstema,
                                            LocalDate opphoerFom, List<VedtakPårørendeSykdomInfotrygd> vedtak) {
        this.tema = tema;
        this.periode = periode;
        this.behandlingstema = behandlingstema;
        this.opphoerFom = opphoerFom;
        this.vedtak = vedtak;
    }


    public Kodeverdi getTema() {
        return tema;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Kodeverdi getBehandlingstema() {
        return behandlingstema;
    }

    public LocalDate getOpphoerFom() {
        return opphoerFom;
    }

    public List<VedtakPårørendeSykdomInfotrygd> getVedtak() {
        return vedtak;
    }

    public static GrunnlagPårørendeSykdomInfotrygd.Builder builder() {
        return new GrunnlagPårørendeSykdomInfotrygd.Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrunnlagPårørendeSykdomInfotrygd that = (GrunnlagPårørendeSykdomInfotrygd) o;
        return Objects.equals(tema, that.tema) && Objects.equals(periode, that.periode) && Objects.equals(behandlingstema, that.behandlingstema) && Objects.equals(opphoerFom, that.opphoerFom) && Objects.equals(vedtak, that.vedtak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tema, periode, behandlingstema, opphoerFom, vedtak);
    }

    @Override
    public String toString() {
        return "GrunnlagPårørendeSykdomInfotrygd{" +
            "tema=" + tema +
            ", periode=" + periode +
            ", behandlingstema=" + behandlingstema +
            ", opphoerFom=" + opphoerFom +
            ", vedtak=" + vedtak +
            '}';
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private Kodeverdi tema;
        private Periode periode;
        private Kodeverdi behandlingstema;
        private LocalDate opphoerFom;
        private List<VedtakPårørendeSykdomInfotrygd> vedtak;

        public GrunnlagPårørendeSykdomInfotrygd build() {
            return new GrunnlagPårørendeSykdomInfotrygd(tema, periode, behandlingstema, opphoerFom, vedtak);
        }

        public GrunnlagPårørendeSykdomInfotrygd.Builder tema(Kodeverdi tema) {
            this.tema = tema;
            return this;
        }


        public GrunnlagPårørendeSykdomInfotrygd.Builder periode(Periode periode) {
            this.periode = periode;
            return this;
        }

        public GrunnlagPårørendeSykdomInfotrygd.Builder opphoerFom(LocalDate opphoerFom) {
            this.opphoerFom = opphoerFom;
            return this;
        }

        public GrunnlagPårørendeSykdomInfotrygd.Builder behandlingstema(Kodeverdi behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public GrunnlagPårørendeSykdomInfotrygd.Builder vedtak(List<VedtakPårørendeSykdomInfotrygd> vedtak) {
            this.vedtak = vedtak;
            return this;
        }
    }

}
