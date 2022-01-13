package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import no.nav.k9.sak.typer.Periode;

@JsonDeserialize(builder = VedtakPårørendeSykdomInfotrygd.Builder.class)
record VedtakPårørendeSykdomInfotrygd (Periode periode, int utbetalingsgrad) {

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private Periode periode;
        private int utbetalingsgrad;

        public VedtakPårørendeSykdomInfotrygd build() {
            return new VedtakPårørendeSykdomInfotrygd(periode, utbetalingsgrad);
        }

        public VedtakPårørendeSykdomInfotrygd.Builder periode(Periode periode) {
            this.periode = periode;
            return this;
        }

        public VedtakPårørendeSykdomInfotrygd.Builder utbetalingsgrad(int utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

    }

}
