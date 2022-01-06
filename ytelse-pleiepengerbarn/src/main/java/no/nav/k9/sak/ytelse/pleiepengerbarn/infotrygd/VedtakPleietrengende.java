package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = VedtakPleietrengende.Builder.class)
class VedtakPleietrengende {
    private final String soekerFnr;
    private final List<Sak> vedtak;

    private VedtakPleietrengende(String soekerFnr, List<Sak> vedtak) {
        this.soekerFnr = soekerFnr;
        this.vedtak = vedtak;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSoekerFnr() {
        return soekerFnr;
    }

    public List<Sak> getVedtak() {
        return vedtak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VedtakPleietrengende that = (VedtakPleietrengende) o;
        return Objects.equals(soekerFnr, that.soekerFnr) &&
            Objects.equals(vedtak, that.vedtak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(soekerFnr, vedtak);
    }

    @Override
    public String toString() {
        return "VedtakBarn{" +
            "soekerFnr='maskert'" +
            ", vedtak=" + vedtak +
            '}';
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String soekerFnr;
        private List<Sak> vedtak;

        public VedtakPleietrengende build() {
            return new VedtakPleietrengende(soekerFnr, vedtak);
        }

        public Builder soekerFnr(String soekerFnr) {
            this.soekerFnr = soekerFnr;
            return this;
        }

        public Builder vedtak(List<Sak> vedtak) {
            this.vedtak = vedtak;
            return this;
        }
    }
}
