package no.nav.foreldrepenger.behandlingslager.uttak;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Table(name = "UTTAK_RESULTAT")
@Entity
public class UttakResultatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_RESULTAT")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "opprinnelig_perioder_id", updatable = false, unique = true)
    private UttakResultatPerioderEntitet opprinneligPerioder;

    @ManyToOne
    @JoinColumn(name = "overstyrt_perioder_id", updatable = false, unique = true)
    private UttakResultatPerioderEntitet overstyrtPerioder;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    public Long getId() {
        return id;
    }

    public UttakResultatPerioderEntitet getOpprinneligPerioder() {
        return opprinneligPerioder;
    }

    public UttakResultatPerioderEntitet getGjeldendePerioder() {
        if (overstyrtPerioder == null && opprinneligPerioder == null) {
            throw new IllegalStateException("Ingen uttaksperioder er satt");
        }
        return overstyrtPerioder != null ? overstyrtPerioder : opprinneligPerioder;
    }

    public void setOpprinneligPerioder(UttakResultatPerioderEntitet opprinneligPerioder) {
        this.opprinneligPerioder = opprinneligPerioder;
    }

    public UttakResultatPerioderEntitet getOverstyrtPerioder() {
        return overstyrtPerioder;
    }

    public void setOverstyrtPerioder(UttakResultatPerioderEntitet overstyrtPerioder) {
        this.overstyrtPerioder = overstyrtPerioder;
    }

    public void deaktiver() {
        aktiv = false;
    }

    public static class Builder {
        private UttakResultatEntitet kladd;

        public Builder(Long behandlingId) {
            kladd = new UttakResultatEntitet();
            kladd.behandlingId = Objects.requireNonNull(behandlingId, "behandlingId");
        }

        public Builder medOpprinneligPerioder(UttakResultatPerioderEntitet opprinneligPerioder) {
            Objects.requireNonNull(opprinneligPerioder);
            kladd.setOpprinneligPerioder(opprinneligPerioder);
            return this;
        }

        public Builder medOverstyrtPerioder(UttakResultatPerioderEntitet overstyrtPerioder) {
            kladd.setOverstyrtPerioder(overstyrtPerioder);
            return this;
        }

        public Builder nullstill() {
            kladd.setOpprinneligPerioder(null);
            kladd.setOverstyrtPerioder(null);
            return this;
        }

        public UttakResultatEntitet build() {
            if (kladd.getOverstyrtPerioder() != null && kladd.getOpprinneligPerioder() == null) {
                throw UttakFeil.FACTORY.manueltFastettePerioderManglerEksisterendeResultat(kladd.behandlingId).toException();
            }
            return kladd;
        }
    }
}
