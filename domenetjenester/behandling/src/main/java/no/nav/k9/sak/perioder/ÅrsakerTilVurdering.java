package no.nav.k9.sak.perioder;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;

public class ÅrsakerTilVurdering {

    private Set<ÅrsakTilVurdering> årsaker;

    private Set<KravDokumentType> kravdokumenterForÅrsaker;

    public ÅrsakerTilVurdering(Set<ÅrsakTilVurdering> årsaker, Set<KravDokumentType> kravdokumenterForÅrsaker) {
        this.årsaker = årsaker;
        this.kravdokumenterForÅrsaker = kravdokumenterForÅrsaker;
    }

    public ÅrsakerTilVurdering(Set<ÅrsakTilVurdering> årsaker) {
        this(årsaker, Collections.emptySet());
    }

    public Set<ÅrsakTilVurdering> getÅrsaker() {
        return årsaker;
    }

    public Set<KravDokumentType> getKravdokumenterForÅrsaker() {
        return kravdokumenterForÅrsaker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ÅrsakerTilVurdering that = (ÅrsakerTilVurdering) o;
        return Objects.equals(årsaker, that.årsaker) && Objects.equals(kravdokumenterForÅrsaker, that.kravdokumenterForÅrsaker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(årsaker, kravdokumenterForÅrsaker);
    }

}
