package no.nav.ung.sak.behandlingslager.behandling.søknad;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.kodeverk.SpråkKodeverdiConverter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "Søknad")
@Table(name = "SO_SOEKNAD")
@DynamicInsert
@DynamicUpdate
public class SøknadEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOEKNAD")
    private Long id;

    @Column(name = "startdato", nullable = false)
    private LocalDate startdato;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    /**
     * eksern Id for mottatt søknad (ikke fra gr_soeknad).
     */
    @Column(name = "soeknad_id")
    private String søknadId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "elektronisk_registrert", nullable = false)
    private boolean elektroniskRegistrert;

    @Column(name = "mottatt_dato")
    private LocalDate mottattDato;

    @Convert(converter = SpråkKodeverdiConverter.class)
    @Column(name = "sprak_kode", nullable = false)
    private Språkkode språkkode = Språkkode.UDEFINERT;

    @Column(name = "begrunnelse_for_sen_innsending")
    private String begrunnelseForSenInnsending;

    SøknadEntitet() {
        // hibernate
    }

    /**
     * Deep copy.
     */
    SøknadEntitet(SøknadEntitet søknadMal) {
        this.begrunnelseForSenInnsending = søknadMal.getBegrunnelseForSenInnsending();
        this.elektroniskRegistrert = søknadMal.getElektroniskRegistrert();
        this.mottattDato = søknadMal.getMottattDato();
        this.startdato = søknadMal.getStartdato();
        if (søknadMal.getSpråkkode() != null) {
            this.språkkode = søknadMal.getSpråkkode();
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDate getStartdato() {
        return startdato;
    }

    void setStartdato(LocalDate startdato) {
        this.startdato = startdato;
    }

    public boolean getElektroniskRegistrert() {
        return elektroniskRegistrert;
    }

    void setElektroniskRegistrert(boolean elektroniskRegistrert) {
        this.elektroniskRegistrert = elektroniskRegistrert;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public Språkkode getSpråkkode() {
        return språkkode;
    }

    void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    public String getBegrunnelseForSenInnsending() {
        return begrunnelseForSenInnsending;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public String getSøknadId() {
        return søknadId;
    }

    void setSøknadId(String søknadId) {
        this.søknadId = søknadId;
    }

    void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    void setBegrunnelseForSenInnsending(String begrunnelseForSenInnsending) {
        this.begrunnelseForSenInnsending = begrunnelseForSenInnsending;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof SøknadEntitet)) {
            return false;
        }
        SøknadEntitet other = (SøknadEntitet) obj;
        return Objects.equals(this.mottattDato, other.mottattDato)
            && Objects.equals(this.startdato, other.startdato)
            && Objects.equals(this.søknadId, other.søknadId)
            && Objects.equals(this.journalpostId, other.journalpostId)
            && Objects.equals(this.begrunnelseForSenInnsending, other.begrunnelseForSenInnsending)
            && Objects.equals(this.språkkode, other.språkkode)
            && Objects.equals(this.elektroniskRegistrert, other.elektroniskRegistrert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elektroniskRegistrert,
            mottattDato,
            journalpostId,
            søknadId,
            startdato,
            språkkode,
            begrunnelseForSenInnsending);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "<startdato=" + startdato //$NON-NLS-1$
            + ", elektroniskRegistrert=" + elektroniskRegistrert
            + ", mottattDato=" + mottattDato
            + ", språkkode=" + språkkode
            + ", journalpostId=" + journalpostId
            + ", søknadId=" + søknadId
            + ", begrunnelseForSenInnsending=" + begrunnelseForSenInnsending
            + ">"; //$NON-NLS-1$
    }

    public static class Builder {
        private SøknadEntitet søknadMal;

        public Builder() {
            this(new SøknadEntitet());
        }

        public Builder(SøknadEntitet søknad) {
            if (søknad != null) {
                this.søknadMal = new SøknadEntitet(søknad);
            } else {
                this.søknadMal = new SøknadEntitet();
            }
        }

        public Builder medElektroniskRegistrert(boolean elektroniskRegistrert) {
            søknadMal.setElektroniskRegistrert(elektroniskRegistrert);
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            søknadMal.setMottattDato(mottattDato);
            return this;
        }

        public Builder medStartdato(LocalDate søknadsdato) {
            søknadMal.setStartdato(søknadsdato);
            return this;
        }

        public Builder medSpråkkode(Språkkode språkkode) {
            søknadMal.setSpråkkode(språkkode);
            return this;
        }

        public Builder medBegrunnelseForSenInnsending(String begrunnelseForSenInnsending) {
            søknadMal.setBegrunnelseForSenInnsending(begrunnelseForSenInnsending);
            return this;
        }

        public Builder medJournalpostId(JournalpostId journalpostId) {
            søknadMal.setJournalpostId(journalpostId);
            return this;
        }

        public Builder medSøknadId(String søknadId) {
            søknadMal.setSøknadId(søknadId);
            return this;
        }

        public SøknadEntitet build() {
            return søknadMal;
        }

    }

}
