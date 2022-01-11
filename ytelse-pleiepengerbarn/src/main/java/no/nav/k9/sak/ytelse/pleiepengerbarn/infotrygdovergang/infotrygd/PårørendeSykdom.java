package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import no.nav.k9.sak.typer.Periode;

@JsonDeserialize(builder = PårørendeSykdom.Builder.class)
record PårørendeSykdom(Kodeverdi tema,
                       Periode periode,
                       Kodeverdi behandlingstema,
                       LocalDate opphoerFom,
                       List<VedtakPårørendeSykdomInfotrygd> vedtak,
                       String foedselsnummerPleietrengende,
                       String foedselsnummerSoeker) {

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private Kodeverdi tema;
        private Periode periode;
        private Kodeverdi behandlingstema;
        private LocalDate opphoerFom;
        private List<VedtakPårørendeSykdomInfotrygd> vedtak;
        private String foedselsnummerPleietrengende;
        private String foedselsnummerSoeker;

        public PårørendeSykdom build() {
            return new PårørendeSykdom(tema, periode, behandlingstema, opphoerFom, vedtak, foedselsnummerPleietrengende, foedselsnummerSoeker);
        }

        public PårørendeSykdom.Builder tema(Kodeverdi tema) {
            this.tema = tema;
            return this;
        }


        public PårørendeSykdom.Builder periode(Periode periode) {
            this.periode = periode;
            return this;
        }

        public PårørendeSykdom.Builder opphoerFom(LocalDate opphoerFom) {
            this.opphoerFom = opphoerFom;
            return this;
        }

        public PårørendeSykdom.Builder behandlingstema(Kodeverdi behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public PårørendeSykdom.Builder vedtak(List<VedtakPårørendeSykdomInfotrygd> vedtak) {
            this.vedtak = vedtak;
            return this;
        }

        public PårørendeSykdom.Builder foedselsnummerPleietrengende(String foedselsnummerPleietrengende) {
            this.foedselsnummerPleietrengende = foedselsnummerPleietrengende;
            return this;
        }

        public PårørendeSykdom.Builder foedselsnummerSoeker(String foedselsnummerSoeker) {
            this.foedselsnummerSoeker = foedselsnummerSoeker;
            return this;
        }

    }

}
