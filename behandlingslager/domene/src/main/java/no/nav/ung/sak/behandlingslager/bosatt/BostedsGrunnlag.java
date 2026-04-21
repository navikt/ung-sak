package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.util.Objects;
import java.util.UUID;

/**
 * Grunnlag som kobler en behandling til bostedsavklarings-aggregatet.
 * Grunnlagsreferansen brukes som nøkkel i Etterlysning-tabellen.
 */
@Entity(name = "BostedsGrunnlag")
@Table(name = "GR_BOSATT_AVKLARING")
public class BostedsGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_BOSATT_AVKLARING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "bosatt_avklaring_holder_id", nullable = false, updatable = false)
    private BostedsAvklaringHolder holder;

    @Column(name = "grunnlag_ref", nullable = false, updatable = false)
    private UUID grunnlagsreferanse;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public BostedsGrunnlag() {
    }

    BostedsGrunnlag(Long behandlingId, BostedsAvklaringHolder holder, UUID grunnlagsreferanse) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(holder, "holder");
        Objects.requireNonNull(grunnlagsreferanse, "grunnlagsreferanse");
        this.behandlingId = behandlingId;
        this.holder = holder;
        this.grunnlagsreferanse = grunnlagsreferanse;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public BostedsAvklaringHolder getHolder() {
        return holder;
    }

    public UUID getGrunnlagsreferanse() {
        return grunnlagsreferanse;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsGrunnlag that)) return false;
        return Objects.equals(holder, that.holder);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(holder);
    }

    @Override
    public String toString() {
        return "BostedsGrunnlag{behandlingId=" + behandlingId
            + ", grunnlagsreferanse=" + grunnlagsreferanse
            + ", aktiv=" + aktiv + '}';
    }
}
