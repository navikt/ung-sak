package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.UtsettelseÅrsak;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public class UtsettelsePeriode implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private UtsettelseÅrsak årsak = UtsettelseÅrsak.UDEFINERT;

    private UtsettelsePeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.årsak = UtsettelseÅrsak.FERIE;
    }

    private UtsettelsePeriode(LocalDate fom, LocalDate tom, UtsettelseÅrsak årsak) {
        this.årsak = årsak;
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    UtsettelsePeriode() {
    }

    UtsettelsePeriode(UtsettelsePeriode utsettelsePeriode) {
        this.periode = utsettelsePeriode.getPeriode();
        this.årsak = utsettelsePeriode.getÅrsak();
    }

    private UtsettelsePeriode(DatoIntervallEntitet datoIntervall, UtsettelseÅrsak årsak) {
        this.årsak = årsak;
        this.periode = datoIntervall;
    }

    public static UtsettelsePeriode ferie(LocalDate fom, LocalDate tom) {
        return new UtsettelsePeriode(fom, tom);
    }

    public static UtsettelsePeriode utsettelse(LocalDate fom, LocalDate tom, UtsettelseÅrsak årsak) {
        return new UtsettelsePeriode(fom, tom, årsak);
    }

    public static UtsettelsePeriode utsettelse(DatoIntervallEntitet datoIntervall, UtsettelseÅrsak årsak) {
        return new UtsettelsePeriode(datoIntervall, årsak);
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { årsak, periode };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Perioden som utsettes
     * @return perioden
     */
    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    /**
     * Årsaken til utsettelsen
     * @return utsettelseårsaken
     */
    public UtsettelseÅrsak getÅrsak() {
        return årsak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UtsettelsePeriode)) return false;
        UtsettelsePeriode that = (UtsettelsePeriode) o;
        return Objects.equals(periode, that.periode) &&
            Objects.equals(årsak, that.årsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, årsak);
    }

    @Override
    public String toString() {
        return "UtsettelsePeriodeEntitet{" +
            "periode=" + periode +
            ", årsak=" + årsak +
            '}';
    }
}
