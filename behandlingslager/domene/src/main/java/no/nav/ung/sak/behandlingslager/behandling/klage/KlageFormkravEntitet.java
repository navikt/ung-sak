package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.util.Objects;

@Table(name = "KLAGE_FORMKRAV")
@Entity(name = "KlageFormkravEntitet")
public class KlageFormkravEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_FORMKRAV")
    private Long id;

    @Column(name = "gjelder_vedtak", nullable = false)
    private boolean gjelderVedtak;

    @Column(name = "er_klager_part", nullable = false)
    private boolean erKlagerPart;

    @Column(name = "er_frist_overholdt", nullable = false)
    private boolean erFristOverholdt;

    @Column(name = "er_konkret", nullable = false)
    private boolean erKonkret;

    @Column(name = "er_signert", nullable = false)
    private boolean erSignert;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    public KlageFormkravEntitet() {
        // Hibernate
    }

    public Long hentId() {
        return id;
    }

    public boolean hentGjelderVedtak() {
        return gjelderVedtak;
    }

    public boolean erKlagerPart() {
        return erKlagerPart;
    }

    public boolean erFristOverholdt() {
        return erFristOverholdt;
    }

    public boolean erKonkret() {
        return erKonkret;
    }

    public boolean erSignert() {
        return erSignert;
    }

    public String hentBegrunnelse() {
        return begrunnelse;
    }

    public void oppdater(KlageFormkravAdapter formkrav) {
        this.erFristOverholdt = formkrav.isErFristOverholdt();
        this.erKlagerPart = formkrav.isErKlagerPart();
        this.erKonkret = formkrav.isErKonkret();
        this.gjelderVedtak = formkrav.gjelderVedtak();
        this.erSignert = formkrav.isErSignert();
        this.begrunnelse = formkrav.getBegrunnelse();
    }

    public KlageFormkravAdapter tilFormkrav() {
        return new KlageFormkravAdapter(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof KlageFormkravEntitet)) {
            return false;
        }
        KlageFormkravEntitet other = (KlageFormkravEntitet) obj;
        return Objects.equals(this.hentId(), other.hentId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private KlageFormkravEntitet klageFormkravEntitetMal;

        public Builder() {
            klageFormkravEntitetMal = new KlageFormkravEntitet();
        }

        public Builder medFormkrav(KlageFormkravAdapter formkrav) {
            klageFormkravEntitetMal.oppdater(formkrav);
            return this;
        }

        public KlageFormkravEntitet build() {
            verifyStateForBuild();
            return klageFormkravEntitetMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(klageFormkravEntitetMal.gjelderVedtak, "gjelderVedtak");
            Objects.requireNonNull(klageFormkravEntitetMal.erKlagerPart, "erKlagerPart");
            Objects.requireNonNull(klageFormkravEntitetMal.erFristOverholdt, "erFristOverholdt");
            Objects.requireNonNull(klageFormkravEntitetMal.erKonkret, "erKonkret");
            Objects.requireNonNull(klageFormkravEntitetMal.erSignert, "erSignert");
            Objects.requireNonNull(klageFormkravEntitetMal.begrunnelse, "begrunnelse");
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "gjelderVedtak=" + hentGjelderVedtak() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "erKlagerPart=" + erKlagerPart() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "erFristOverholdt=" + erFristOverholdt() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "erKonkret=" + erKonkret() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "erSignert=" + erSignert() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "begrunnelse=" + hentBegrunnelse() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }
}
