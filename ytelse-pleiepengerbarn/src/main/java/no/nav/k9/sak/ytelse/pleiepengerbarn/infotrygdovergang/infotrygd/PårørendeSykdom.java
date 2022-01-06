package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = PårørendeSykdom.Builder.class)
record PårørendeSykdom(GrunnlagPårørendeSykdomInfotrygd generelt, String foedselsnummerPleietrengende) {

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private GrunnlagPårørendeSykdomInfotrygd generelt;
        private String foedselsnummerPleietrengende;

        public PårørendeSykdom build() {
            return new PårørendeSykdom(generelt, foedselsnummerPleietrengende);
        }

        public PårørendeSykdom.Builder generelt(GrunnlagPårørendeSykdomInfotrygd generelt) {
            this.generelt = generelt;
            return this;
        }

        public PårørendeSykdom.Builder foedselsnummerPleietrengende(String foedselsnummerPleietrengende) {
            this.foedselsnummerPleietrengende = foedselsnummerPleietrengende;
            return this;
        }
    }

}
