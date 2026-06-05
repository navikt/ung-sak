package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregat for bostedsavklaring knyttet til én vilkårsperiode.
 * {@code skjæringstidspunkt} tilsvarer fom-dato for vilkårsperioden og matcher
 * {@code referanse} referanse for å kunne garantere at etterlysning/uttalelse linkes til riktig vurdering
 * {@code erBosattITrondheim} angir om bruker er bosatt ved skjæringstidspunktet.
 * {@code fraflyttingsDato} angir eventuell dato for utflytting fra Trondheim (null dersom bruker ikke har flyttet ut).
 * {@code fraflyttingsÅrsak} angir årsaken til fraflytting (null dersom bruker er bosatt hele perioden).
 */
@Entity(name = "BostedsPeriodeAvklaring")
@Table(name = "BOSATT_PERIODE_AVKLARING")
@Immutable
public class BostedsPeriodeAvklaring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_PERIODE_AVKLARING")
    private Long id;

    @Column(name = "referanse", nullable = false, updatable = false)
    private UUID referanse = UUID.randomUUID();

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "er_bosatt_i_trondheim", nullable = false, updatable = false)
    private boolean erBosattITrondheim;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraflyttings_aarsak", updatable = false)
    private FraflyttingsÅrsak fraflyttingsÅrsak;

    @Enumerated(EnumType.STRING)
    @Column(name = "kilde", nullable = false, updatable = false)
    private Kilde kilde;

    public BostedsPeriodeAvklaring() {
        // Hibernate
    }

    public BostedsPeriodeAvklaring(DatoIntervallEntitet periode, boolean erBosattITrondheim, FraflyttingsÅrsak fraflyttingsÅrsak, Kilde kilde) {
        this.periode = periode.toRange();
        this.erBosattITrondheim = erBosattITrondheim;
        this.fraflyttingsÅrsak = fraflyttingsÅrsak;
        this.kilde = kilde;
    }

    public Long getId() {
        return id;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public boolean isErBosattITrondheim() {
        return erBosattITrondheim;
    }

    public FraflyttingsÅrsak getFraflyttingsÅrsak() {
        return fraflyttingsÅrsak;
    }

    public Kilde getKilde() {
        return kilde;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsPeriodeAvklaring that)) return false;
        return erBosattITrondheim == that.erBosattITrondheim
            && fraflyttingsÅrsak == that.fraflyttingsÅrsak
            && kilde == that.kilde;
    }

    @Override
    public int hashCode() {
        return Objects.hash(erBosattITrondheim, fraflyttingsÅrsak, kilde);
    }

    @Override
    public String toString() {
        return "BostedsPeriodeAvklaring{referanse=" + referanse
            + ", erBosattITrondheim=" + erBosattITrondheim
            + ", fraflyttingsÅrsak=" + fraflyttingsÅrsak
            + ", kilde=" + kilde + '}';
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }
}
