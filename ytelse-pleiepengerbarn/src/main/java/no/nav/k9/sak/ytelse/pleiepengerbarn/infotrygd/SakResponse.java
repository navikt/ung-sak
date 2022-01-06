package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = SakResponse.Builder.class)
class SakResponse {
    private final List<Sak> saker;
    private final List<Sak> vedtak;

    private SakResponse(List<Sak> saker, List<Sak> vedtak) {
        this.saker = saker;
        this.vedtak = vedtak;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Sak> getSaker() {
        return saker;
    }

    public List<Sak> getVedtak() {
        return vedtak;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private List<Sak> saker = List.of();
        private List<Sak> vedtak = List.of();

        public SakResponse build() {
            return new SakResponse(List.copyOf(saker), List.copyOf(vedtak));
        }

        public Builder saker(List<Sak> saker) {
            this.saker = saker;
            return this;
        }

        public Builder vedtak(List<Sak> vedtak) {
            this.vedtak = vedtak;
            return this;
        }
    }
}
