package no.nav.ung.sak.ytelseperioder;

import java.time.LocalDate;
import java.util.Objects;

public record YtelsesperiodeDefinisjon(String definisjon) {

    public static YtelsesperiodeDefinisjon fraFomDato(LocalDate fomDato) {
        return new YtelsesperiodeDefinisjon(fomDato.getMonth().name());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof YtelsesperiodeDefinisjon that)) return false;
        return Objects.equals(definisjon, that.definisjon);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(definisjon);
    }
}
