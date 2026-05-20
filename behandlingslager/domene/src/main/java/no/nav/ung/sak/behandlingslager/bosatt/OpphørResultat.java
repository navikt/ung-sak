package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "OpphørResultat")
@Table(name = "RS_OPPHOER")
public class OpphørResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RS_OPPHOER")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "skaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "opphors_dato", updatable = false)
    private LocalDate opphørsDato;

    @Enumerated(EnumType.STRING)
    @Column(name = "opphors_aarsak", updatable = false)
    private Avslagsårsak opphørsÅrsak;

    @Enumerated(EnumType.STRING)
    @Column(name = "kilde", nullable = false, updatable = false)
    private Kilde kilde;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public OpphørResultat() {
        // Hibernate
    }

    public OpphørResultat(Long behandlingId, LocalDate skjæringstidspunkt, LocalDate opphørsDato, Avslagsårsak opphørsÅrsak, Kilde kilde) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(skjæringstidspunkt, "skjæringstidspunkt");
        Objects.requireNonNull(kilde, "kilde");
        this.behandlingId = behandlingId;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.opphørsDato = opphørsDato;
        this.opphørsÅrsak = opphørsÅrsak;
        this.kilde = kilde;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public LocalDate getOpphørsDato() {
        return opphørsDato;
    }

    public Avslagsårsak getOpphørsÅrsak() {
        return opphørsÅrsak;
    }

    public Kilde getKilde() {
        return kilde;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpphørResultat that)) return false;
        return Objects.equals(behandlingId, that.behandlingId)
            && Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt)
            && Objects.equals(opphørsDato, that.opphørsDato)
            && opphørsÅrsak == that.opphørsÅrsak
            && kilde == that.kilde;
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, skjæringstidspunkt, opphørsDato, opphørsÅrsak, kilde);
    }

    @Override
    public String toString() {
        return "OpphørResultat{behandlingId=" + behandlingId
            + ", skjæringstidspunkt=" + skjæringstidspunkt
            + ", opphørsDato=" + opphørsDato
            + ", opphørsÅrsak=" + opphørsÅrsak
            + ", kilde=" + kilde
            + ", aktiv=" + aktiv + '}';
    }
}
