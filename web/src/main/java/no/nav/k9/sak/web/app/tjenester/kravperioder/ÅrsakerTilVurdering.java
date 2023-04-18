package no.nav.k9.sak.web.app.tjenester.kravperioder;

import java.util.Objects;
import java.util.Set;

import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;

public class ÅrsakerTilVurdering {

    private Set<ÅrsakTilVurdering> årsaker;

    public ÅrsakerTilVurdering(Set<ÅrsakTilVurdering> årsaker) {
        this.årsaker = årsaker;
    }

    public Set<ÅrsakTilVurdering> getÅrsaker() {
        return årsaker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ÅrsakerTilVurdering that = (ÅrsakerTilVurdering) o;
        return Objects.equals(årsaker, that.årsaker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(årsaker);
    }
}
