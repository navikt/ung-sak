package no.nav.k9.sak.behandlingslager.behandling.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Entitet som representerer Opptjening. Denne har også et sett med {@link OpptjeningAktivitet}.
 * Grafen her er immutable og tillater ikke endring av data elementer annet enn metadata (aktiv flagg osv.)
 * {@link OpptjeningRepository} besørger riktig oppdatering og skriving, og oppretting av nytt innslag ved hver endring.
 */
@Entity(name = "Opptjening")
@Table(name = "OPPTJENING")
@DynamicInsert
@DynamicUpdate
public class Opptjening extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPTJENING")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "opptjening_resultat_id", nullable = false, updatable = false)
    private OpptjeningResultat opptjeningResultat;

    @ChangeTracked
    @Embedded
    private DatoIntervallEntitet opptjeningPeriode;

    /* Mapper kun fra denne og ikke bi-directional, gjør vedlikehold enklere. */
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true /* ok siden aktiviteter er eid av denne */)
    @JoinColumn(name = "OPPTJENINGSPERIODE_ID", nullable = false, updatable = false)
    private List<OpptjeningAktivitet> opptjeningAktivitet = new ArrayList<>();

    @ChangeTracked
    @Column(name = "opptjent_periode")
    private String opptjentPeriode;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public Opptjening(LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fom, "opptjeningsperiodeFom");
        Objects.requireNonNull(tom, "opptjeningsperiodeTom");
        this.opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    /**
     * copy-constructor.
     */
    public Opptjening(Opptjening annen) {
        this.opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(annen.getFom(), annen.getTom());
        this.opptjentPeriode = annen.getOpptjentPeriode() == null ? null : annen.getOpptjentPeriode().toString();
        this.opptjeningAktivitet
            .addAll(annen.getOpptjeningAktivitet().stream().map(oa -> new OpptjeningAktivitet(oa)).collect(Collectors.toList()));
        // kopierer ikke data som ikke er relevante (aktiv, versjon, id, etc)

    }

    Opptjening() {
        // for hibernate
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Opptjening)) {
            return false;
        }
        Opptjening other = (Opptjening) obj;
        return Objects.equals(this.getFom(), other.getFom())
            && Objects.equals(this.getTom(), other.getTom());
    }

    public LocalDate getFom() {
        return opptjeningPeriode.getFomDato();
    }

    public Long getId() {
        return id;
    }

    public List<OpptjeningAktivitet> getOpptjeningAktivitet() {
        // alle returnerte data fra denne klassen skal være immutable
        return Collections.unmodifiableList(opptjeningAktivitet);
    }

    public void setOpptjeningAktivitet(Collection<OpptjeningAktivitet> opptjeningAktivitet) {
        this.opptjeningAktivitet.clear();
        this.opptjeningAktivitet.addAll(opptjeningAktivitet);
    }

    public Period getOpptjentPeriode() {
        return opptjentPeriode == null ? null : Period.parse(opptjentPeriode);
    }

    void setOpptjentPeriode(Period opptjentPeriode) {
        this.opptjentPeriode = opptjentPeriode == null ? null : opptjentPeriode.toString();
    }

    public LocalDate getTom() {
        return opptjeningPeriode.getTomDato();
    }

    /**
     * Returnerer skjæringstidspunktet for opptjening.
     * T.o.m. dato for opptjeningsperioden plus 1 dag
     *
     * @return opptjeningsperioden t.o.m. + 1 dag
     */
    public LocalDate getSkjæringstidspunkt() {
        return getTom().plusDays(1);
    }

    /**
     * fom/tom opptjening er gjort.
     */
    public DatoIntervallEntitet getOpptjeningPeriode() {
        return opptjeningPeriode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(opptjeningPeriode);
    }

    OpptjeningResultat getOpptjeningResultat() {
        return opptjeningResultat;
    }

    void setOpptjeningResultat(OpptjeningResultat opptjeningResultat) {
        this.opptjeningResultat = opptjeningResultat;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id + ", "
            + "opptjeningsperiodeFom=" + opptjeningPeriode.getFomDato() + ", "
            + "opptjeningsperiodeTom=" + opptjeningPeriode.getTomDato()
            + (opptjentPeriode == null ? "" : ", opptjentPeriode=" + opptjentPeriode)
            + ">";
    }
}
