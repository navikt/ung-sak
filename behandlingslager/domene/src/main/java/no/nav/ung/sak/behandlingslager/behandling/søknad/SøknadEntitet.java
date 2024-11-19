package no.nav.ung.sak.behandlingslager.behandling.søknad;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.kodeverk.RelasjonsRolleTypeKodeverdiConverter;
import no.nav.ung.sak.behandlingslager.kodeverk.SpråkKodeverdiConverter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;

@Entity(name = "Søknad")
@Table(name = "SO_SOEKNAD")
@DynamicInsert
@DynamicUpdate
public class SøknadEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOEKNAD")
    private Long id;

    @Column(name = "soeknadsdato", nullable = false)
    private LocalDate søknadsdato;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    /** eksern Id for mottatt søknad (ikke fra gr_soeknad). */
    @Column(name = "soeknad_id")
    private String søknadId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "elektronisk_registrert", nullable = false)
    private boolean elektroniskRegistrert;

    @Column(name = "mottatt_dato")
    private LocalDate mottattDato;

    @Column(name = "tilleggsopplysninger")
    private String tilleggsopplysninger;

    @Convert(converter = SpråkKodeverdiConverter.class)
    @Column(name = "sprak_kode", nullable = false)
    private Språkkode språkkode = Språkkode.UDEFINERT;

    @Column(name = "begrunnelse_for_sen_innsending")
    private String begrunnelseForSenInnsending;

    @Column(name = "er_endringssoeknad", nullable = false)
    private boolean erEndringssøknad;

    @Convert(converter = RelasjonsRolleTypeKodeverdiConverter.class)
    @Column(name = "bruker_rolle", nullable = false)
    private RelasjonsRolleType brukerRolle = RelasjonsRolleType.UDEFINERT;

    @OneToMany(cascade = { CascadeType.ALL }, mappedBy = "søknad")
    private Set<SøknadAngittPersonEntitet> angittePersoner = new HashSet<>(2);

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "fom", updatable = false)),
            @AttributeOverride(name = "tomDato", column = @Column(name = "tom", updatable = false))
    })
    private DatoIntervallEntitet periode;

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
        this.søknadsdato = søknadMal.getSøknadsdato();
        this.erEndringssøknad = søknadMal.erEndringssøknad();
        this.tilleggsopplysninger = søknadMal.getTilleggsopplysninger();
        this.periode = søknadMal.getSøknadsperiode();
        if (søknadMal.getSpråkkode() != null) {
            this.språkkode = søknadMal.getSpråkkode();
        }
        this.brukerRolle = søknadMal.getRelasjonsRolleType();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSøknadsdato() {
        return søknadsdato;
    }

    void setSøknadsdato(LocalDate søknadsdato) {
        this.søknadsdato = søknadsdato;
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

    public String getTilleggsopplysninger() {
        return tilleggsopplysninger;
    }

    void setTilleggsopplysninger(String tilleggsopplysninger) {
        this.tilleggsopplysninger = tilleggsopplysninger;
    }

    public Set<SøknadAngittPersonEntitet> getAngittePersoner() {
        return angittePersoner;
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

    public boolean erEndringssøknad() {
        return erEndringssøknad;
    }

    void setErEndringssøknad(boolean endringssøknad) {
        this.erEndringssøknad = endringssøknad;
    }

    void setRelasjonsRolleType(RelasjonsRolleType brukerRolle) {
        this.brukerRolle = brukerRolle;
    }

    void setSøknadsperiode(DatoIntervallEntitet søknadsperiode) {
        this.periode = søknadsperiode;
    }

    public DatoIntervallEntitet getSøknadsperiode() {
        return periode;
    }

    public RelasjonsRolleType getRelasjonsRolleType() {
        return brukerRolle;
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
            && Objects.equals(this.søknadsdato, other.søknadsdato)
            && Objects.equals(this.søknadId, other.søknadId)
            && Objects.equals(this.journalpostId, other.journalpostId)
            && Objects.equals(this.periode, other.periode)
            && Objects.equals(this.begrunnelseForSenInnsending, other.begrunnelseForSenInnsending)
            && Objects.equals(this.erEndringssøknad, other.erEndringssøknad)
            && Objects.equals(this.språkkode, other.språkkode)
            && Objects.equals(this.tilleggsopplysninger, other.tilleggsopplysninger)
            && Objects.equals(this.elektroniskRegistrert, other.elektroniskRegistrert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elektroniskRegistrert,
            mottattDato,
            erEndringssøknad,
            journalpostId,
            søknadId,
            søknadsdato,
            tilleggsopplysninger,
            språkkode,
            periode,
            begrunnelseForSenInnsending);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "<termindato=" + søknadsdato //$NON-NLS-1$
            + ", elektroniskRegistrert=" + elektroniskRegistrert
            + ", mottattDato=" + mottattDato
            + ", erEndringssøknad=" + erEndringssøknad
            + ", tilleggsopplysninger=" + tilleggsopplysninger
            + ", språkkode=" + språkkode
            + ", søknadperiode=" + periode
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

        public Builder medSøknadsdato(LocalDate søknadsdato) {
            søknadMal.setSøknadsdato(søknadsdato);
            return this;
        }

        public Builder medTilleggsopplysninger(String tilleggsopplysninger) {
            søknadMal.setTilleggsopplysninger(tilleggsopplysninger);
            return this;
        }

        public Builder medSpråkkode(Språkkode språkkode) {
            søknadMal.setSpråkkode(språkkode);
            return this;
        }

        public Builder leggTilAngittPerson(SøknadAngittPersonEntitet angitt) {
            var sve = new SøknadAngittPersonEntitet(angitt);
            søknadMal.angittePersoner.add(sve);
            sve.setSøknad(søknadMal);
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

        public Builder medRelasjonsRolleType(RelasjonsRolleType relasjonsRolleType) {
            søknadMal.setRelasjonsRolleType(relasjonsRolleType);
            return this;
        }

        public Builder medErEndringssøknad(boolean erEndringssøknad) {
            søknadMal.setErEndringssøknad(erEndringssøknad);
            return this;
        }

        public Builder medSøknadsperiode(DatoIntervallEntitet søknadsperiode) {
            søknadMal.setSøknadsperiode(søknadsperiode);
            return this;
        }

        public Builder medSøknadId(String søknadId) {
            søknadMal.setSøknadId(søknadId);
            return this;
        }

        public SøknadEntitet build() {
            return søknadMal;
        }

        public Builder medSøknadsperiode(LocalDate fom, LocalDate tom) {
            return medSøknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        }
    }

}
