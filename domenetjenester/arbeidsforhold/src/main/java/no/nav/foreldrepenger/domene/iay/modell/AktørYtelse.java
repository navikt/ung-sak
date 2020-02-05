package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.typer.AktørId;

public class AktørYtelse implements IndexKey {

    private AktørId aktørId;

    @ChangeTracked
    private Set<Ytelse> ytelser = new LinkedHashSet<>();

    AktørYtelse() {
    }

    /**
     * Deep copy ctor
     */
    AktørYtelse(AktørYtelse aktørYtelse) {
        this.aktørId = aktørYtelse.getAktørId();
        this.ytelser = aktørYtelse.getAlleYtelser().stream().map(ytelse -> {
            var yt = new Ytelse(ytelse);
            return yt;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { getAktørId() };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Aktøren tilstøtende ytelser gjelder for
     * @return aktørId
     */
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    /**
     * Alle registrerte tilstøende ytelser (ufiltrert).
     */
    public Collection<Ytelse> getAlleYtelser() {
        return List.copyOf(ytelser);
    }

    boolean hasValues() {
        return aktørId != null || ytelser != null && !ytelser.isEmpty();
    }

    void leggTilYtelse(Ytelse ytelse) {
        this.ytelser.add(ytelse);
    }

    void fjernYtelse(Ytelse ytelse) {
        this.ytelser.remove(ytelse);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof AktørYtelse)) {
            return false;
        }
        AktørYtelse other = (AktørYtelse) obj;
        return Objects.equals(this.getAktørId(), other.getAktørId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "aktørId=" + aktørId +
            ", ytelser=" + ytelser +
            '>';
    }
}
