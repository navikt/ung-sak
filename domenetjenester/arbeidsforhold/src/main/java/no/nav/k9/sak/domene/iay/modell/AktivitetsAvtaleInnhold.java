package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.Stillingsprosent;

public class AktivitetsAvtaleInnhold {

    @ChangeTracked
    private Stillingsprosent prosentsats;
    private String beskrivelse;
    @ChangeTracked
    private LocalDate sisteLønnsendringsdato;

    AktivitetsAvtaleInnhold() {
    }

    /**
     * Deep copy ctor
     */
    AktivitetsAvtaleInnhold(AktivitetsAvtaleInnhold aktivitetsAvtale) {
        this.prosentsats = aktivitetsAvtale.getProsentsats();
        this.beskrivelse = aktivitetsAvtale.getBeskrivelse();
        this.sisteLønnsendringsdato = aktivitetsAvtale.getSisteLønnsendringsdato();
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

    public LocalDate getSisteLønnsendringsdato() {
        return sisteLønnsendringsdato;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    void setSisteLønnsendringsdato(LocalDate sisteLønnsendringsdato) {
        this.sisteLønnsendringsdato = sisteLønnsendringsdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || o.getClass() != this.getClass())
            return false;

        var that = (AktivitetsAvtaleInnhold) o;
        return Objects.equals(beskrivelse, that.beskrivelse) &&
            Objects.equals(prosentsats, that.prosentsats) &&
            Objects.equals(sisteLønnsendringsdato, that.sisteLønnsendringsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beskrivelse, prosentsats, sisteLønnsendringsdato);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (prosentsats == null ? "" : ", prosentsats=" + prosentsats) + //$NON-NLS-1$
            (beskrivelse == null ? "" : ", beskrivelse=" + beskrivelse) + //$NON-NLS-1$
            (sisteLønnsendringsdato == null ? "" : ", sisteLønnsendringsdato=" + sisteLønnsendringsdato) + //$NON-NLS-1$
            ", erAnsettelsesPeriode=" + erAnsettelsesPeriode() +
            '>';
    }
    public boolean erAnsettelsesPeriode() {
        return (prosentsats == null || prosentsats.erNulltall())
            && sisteLønnsendringsdato == null;
    }

}
