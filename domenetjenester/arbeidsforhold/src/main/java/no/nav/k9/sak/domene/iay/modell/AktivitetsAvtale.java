package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.vedtak.konfig.Tid;

public class AktivitetsAvtale implements IndexKey {

    @ChangeTracked
    private Stillingsprosent prosentsats;

    private String beskrivelse;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private LocalDate sisteLønnsendringsdato;

    /**
     * Setter en periode brukt til overstyring av angitt periode (avledet fra saksbehandlers vurderinger). Benyttes kun transient (ved filtrering av modellen)
     */
    private DatoIntervallEntitet overstyrtPeriode;

    AktivitetsAvtale() {
    }

    /**
     * Deep copy ctor
     */
    AktivitetsAvtale(AktivitetsAvtale aktivitetsAvtale) {
        this.prosentsats = aktivitetsAvtale.getProsentsats();
        this.beskrivelse = aktivitetsAvtale.getBeskrivelse();
        this.sisteLønnsendringsdato = aktivitetsAvtale.getSisteLønnsendringsdato();
        this.periode = aktivitetsAvtale.getPeriodeUtenOverstyring();
    }

    public AktivitetsAvtale(AktivitetsAvtale avtale, DatoIntervallEntitet overstyrtPeriode) {
        this(avtale);
        this.overstyrtPeriode = overstyrtPeriode;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, sisteLønnsendringsdato };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Avtalt prosentsats i avtalen
     *
     * @return prosent
     */

    public Stillingsprosent getProsentsats() {
        return prosentsats;
    }

    void setProsentsats(Stillingsprosent prosentsats) {
        this.prosentsats = prosentsats;
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
        return sisteLønnsendringsdato;
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
        return beskrivelse;
    }

    void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    void sisteLønnsendringsdato(LocalDate sisteLønnsendringsdato) {
        this.sisteLønnsendringsdato = sisteLønnsendringsdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AktivitetsAvtale)) return false;
        AktivitetsAvtale that = (AktivitetsAvtale) o;
        return 
            Objects.equals(beskrivelse, that.beskrivelse) &&
            Objects.equals(prosentsats, that.prosentsats) &&
            Objects.equals(periode, that.periode) &&
            Objects.equals(sisteLønnsendringsdato, that.sisteLønnsendringsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beskrivelse, prosentsats, periode, sisteLønnsendringsdato);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            "periode=" + periode + //$NON-NLS-1$
            ", overstyrtPeriode=" + overstyrtPeriode + //$NON-NLS-1$
            ", prosentsats=" + prosentsats + //$NON-NLS-1$
            ", beskrivelse=" + beskrivelse + //$NON-NLS-1$
            ", sisteLønnsendringsdato="+sisteLønnsendringsdato + //$NON-NLS-1$
            '>';
    }

    boolean hasValues() {
        return prosentsats != null || periode != null;
    }


    public boolean erAnsettelsesPeriode() {
        return (prosentsats == null || prosentsats.erNulltall())
            && sisteLønnsendringsdato == null;
    }

}
