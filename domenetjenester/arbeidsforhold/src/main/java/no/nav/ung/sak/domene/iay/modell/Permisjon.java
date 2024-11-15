package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Stillingsprosent;

public class Permisjon implements IndexKey {

    private Yrkesaktivitet yrkesaktivitet;

    private PermisjonsbeskrivelseType permisjonsbeskrivelseType;

    private DatoIntervallEntitet periode;

    @ChangeTracked
    private Stillingsprosent prosentsats;

    Permisjon() {
    }

    /**
     * Deep copy ctor
     */
    Permisjon(Permisjon permisjon) {
        this.permisjonsbeskrivelseType = permisjon.getPermisjonsbeskrivelseType();
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(permisjon.getFraOgMed(), permisjon.getTilOgMed());
        this.prosentsats = permisjon.getProsentsats();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, getPermisjonsbeskrivelseType() };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Beskrivelse av permisjonen
     *
     * @return {@link PermisjonsbeskrivelseType}
     */
    public PermisjonsbeskrivelseType getPermisjonsbeskrivelseType() {
        return permisjonsbeskrivelseType;
    }

    void setPermisjonsbeskrivelseType(PermisjonsbeskrivelseType permisjonsbeskrivelseType) {
        this.permisjonsbeskrivelseType = permisjonsbeskrivelseType;
    }

    void setPeriode(LocalDate fraOgMed, LocalDate tilOgMed) {
        if (tilOgMed != null) {
            this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        } else {
            this.periode = DatoIntervallEntitet.fraOgMed(fraOgMed);
        }
    }

    public LocalDate getFraOgMed() {
        return periode.getFomDato();
    }

    public LocalDate getTilOgMed() {
        return periode.getTomDato();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    /**
     * Prosentsats som aktøren er permitert fra arbeidet
     *
     * @return prosentsats
     */
    public Stillingsprosent getProsentsats() {
        return prosentsats;
    }

    void setProsentsats(Stillingsprosent prosentsats) {
        this.prosentsats = prosentsats;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Permisjon)) {
            return false;
        }
        Permisjon other = (Permisjon) obj;
        return Objects.equals(this.permisjonsbeskrivelseType, other.permisjonsbeskrivelseType)
            && Objects.equals(this.periode, other.periode)
            && Objects.equals(this.prosentsats, other.prosentsats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permisjonsbeskrivelseType, periode, prosentsats);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "permisjonsbeskrivelseType=" + permisjonsbeskrivelseType +
            ", periode=" + periode +
            ", prosentsats=" + prosentsats +
            '>';
    }

    public Yrkesaktivitet getYrkesaktivitet() {
        return yrkesaktivitet;
    }

    void setYrkesaktivitet(Yrkesaktivitet yrkesaktivitet) {
        this.yrkesaktivitet = yrkesaktivitet;
    }

    boolean hasValues() {
        return permisjonsbeskrivelseType != null || periode.getFomDato() != null || prosentsats != null;
    }
}
