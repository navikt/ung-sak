package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Stillingsprosent;

public class AktivitetsAvtale implements IndexKey {

    public static final Comparator<AktivitetsAvtale> COMPARATOR = Comparator.comparing(AktivitetsAvtale::getPeriode)
        .thenComparing(AktivitetsAvtale::getSisteLønnsendringsdato, Comparator.nullsFirst(Comparator.naturalOrder()));
    @ChangeTracked
    private AktivitetsAvtaleInnhold aktivitetsAvtaleInnhold = new AktivitetsAvtaleInnhold();


    @ChangeTracked
    private DatoIntervallEntitet periode;

    /**
     * Setter en periode brukt til overstyring av angitt periode (avledet fra saksbehandlers vurderinger). Benyttes kun transient (ved
     * filtrering av modellen)
     */
    private DatoIntervallEntitet overstyrtPeriode;

    AktivitetsAvtale() {
    }

    /**
     * Deep copy ctor
     */
    AktivitetsAvtale(AktivitetsAvtale aktivitetsAvtale) {
        this.aktivitetsAvtaleInnhold = new AktivitetsAvtaleInnhold(aktivitetsAvtale.aktivitetsAvtaleInnhold);
        this.periode = aktivitetsAvtale.getPeriodeUtenOverstyring();
    }

    public AktivitetsAvtale(AktivitetsAvtale avtale, DatoIntervallEntitet overstyrtPeriode) {
        this(avtale);
        this.overstyrtPeriode = overstyrtPeriode;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, aktivitetsAvtaleInnhold.getSisteLønnsendringsdato() };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Avtalt prosentsats i avtalen
     *
     * @return prosent
     */

    public Stillingsprosent getProsentsats() {
        return aktivitetsAvtaleInnhold.getProsentsats();
    }

    void setProsentsats(Stillingsprosent prosentsats) {
        this.aktivitetsAvtaleInnhold.setProsentsats(prosentsats);
    }

    /**
     * Perioden til aktivitetsavtalen.
     * Tar hensyn til overstyring gjort i 5080.
     *
     * @return Hele perioden, tar hensyn til overstyringer.
     */
    public DatoIntervallEntitet getPeriode() {
        return erOverstyrtPeriode() ? overstyrtPeriode : periode;
    }

    /**
     * Henter kun den originale perioden, ikke den overstyrte perioden.
     * Bruk heller {@link #getPeriode} i de fleste tilfeller
     *
     * @return Hele den originale perioden, uten overstyringer.
     */

    public DatoIntervallEntitet getPeriodeUtenOverstyring() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    /**
     * Hvorvidet denne avtalen har en overstyrt periode.
     */

    public boolean erOverstyrtPeriode() {
        return overstyrtPeriode != null;
    }

    public LocalDate getSisteLønnsendringsdato() {
        return aktivitetsAvtaleInnhold.getSisteLønnsendringsdato();
    }

    public boolean matcherPeriode(DatoIntervallEntitet aktivitetsAvtale) {
        return getPeriode().equals(aktivitetsAvtale);
    }

    /**
     * Er avtallen løpende
     *
     * @return true/false
     */
    public boolean getErLøpende() {
        return Tid.TIDENES_ENDE.equals(getPeriode().getTomDato());
    }

    public String getBeskrivelse() {
        return aktivitetsAvtaleInnhold.getBeskrivelse();
    }

    void setBeskrivelse(String beskrivelse) {
        this.aktivitetsAvtaleInnhold.setBeskrivelse(beskrivelse);
    }

    void setSisteLønnsendringsdato(LocalDate sisteLønnsendringsdato) {
        this.aktivitetsAvtaleInnhold.setSisteLønnsendringsdato(sisteLønnsendringsdato);
    }

    public AktivitetsAvtaleInnhold getAktivitetsAvtaleInnhold() {
        return aktivitetsAvtaleInnhold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || o.getClass() != this.getClass())
            return false;

        var that = (AktivitetsAvtale) o;
        return Objects.equals(aktivitetsAvtaleInnhold, that.aktivitetsAvtaleInnhold) &&
            Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, aktivitetsAvtaleInnhold);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            "periode=" + periode + //$NON-NLS-1$
            (overstyrtPeriode == null ? "" : ", overstyrtPeriode=" + overstyrtPeriode) + //$NON-NLS-1$
            (aktivitetsAvtaleInnhold == null ? "" : ", aktivitetsAvtaleInnhold=" + aktivitetsAvtaleInnhold) + //$NON-NLS-1$
            ", erAnsettelsesPeriode=" + erAnsettelsesPeriode() +
            '>';
    }

    boolean hasValues() {
        return aktivitetsAvtaleInnhold.getProsentsats() != null || periode != null;
    }

    public boolean erAnsettelsesPeriode() {
        return aktivitetsAvtaleInnhold == null || ((aktivitetsAvtaleInnhold.getProsentsats() == null || aktivitetsAvtaleInnhold.getProsentsats().erNulltall())
            && aktivitetsAvtaleInnhold.getSisteLønnsendringsdato() == null);
    }

}
