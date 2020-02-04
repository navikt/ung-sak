package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.RelasjonsRolleTypeKodeverdiConverter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.SpråkKodeverdiConverter;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;

@Entity(name = "Søknad")
@Table(name = "SO_SOEKNAD")
public class SøknadEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOEKNAD")
    private Long id;

    @Column(name = "soeknadsdato", nullable = false)
    private LocalDate søknadsdato;

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

    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "søknad")
    private Set<SøknadVedleggEntitet> søknadVedlegg = new HashSet<>(2);

    @Column(name = "begrunnelse_for_sen_innsending")
    private String begrunnelseForSenInnsending;

    
    @Column(name = "er_endringssoeknad", nullable = false)
    private boolean erEndringssøknad;


    @Convert(converter = RelasjonsRolleTypeKodeverdiConverter.class)
    @Column(name="bruker_rolle", nullable = false)
    private RelasjonsRolleType brukerRolle = RelasjonsRolleType.UDEFINERT;

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

        if (søknadMal.getSpråkkode() != null) {
            this.språkkode = søknadMal.getSpråkkode();
        }
        for (SøknadVedleggEntitet aSøknadVedlegg : søknadMal.getSøknadVedlegg()) {
            SøknadVedleggEntitet kopi = new SøknadVedleggEntitet(aSøknadVedlegg);
            kopi.setSøknad(this);
            this.søknadVedlegg.add(kopi);
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


    public Språkkode getSpråkkode() {
        return språkkode;
    }

    public void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    public Set<SøknadVedleggEntitet> getSøknadVedlegg() {
        return Collections.unmodifiableSet(søknadVedlegg);
    }


    public String getBegrunnelseForSenInnsending() {
        return begrunnelseForSenInnsending;
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
                && Objects.equals(this.tilleggsopplysninger, other.tilleggsopplysninger)
                && Objects.equals(this.søknadVedlegg, other.søknadVedlegg)
                && Objects.equals(this.begrunnelseForSenInnsending, other.begrunnelseForSenInnsending)
                && Objects.equals(this.erEndringssøknad, other.erEndringssøknad)
                && Objects.equals(this.språkkode, other.språkkode)
                && Objects.equals(this.elektroniskRegistrert, other.elektroniskRegistrert);
    }


    @Override
    public int hashCode() {
        return Objects.hash(elektroniskRegistrert, 
            mottattDato, 
            erEndringssøknad,
            søknadsdato, 
            tilleggsopplysninger, 
            språkkode, 
            søknadVedlegg, 
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

        public Builder leggTilVedlegg(SøknadVedleggEntitet søknadVedlegg) {
            SøknadVedleggEntitet sve = new SøknadVedleggEntitet(søknadVedlegg);
            søknadMal.søknadVedlegg.add(sve);
            sve.setSøknad(søknadMal);
            return this;
        }

        public Builder medBegrunnelseForSenInnsending(String begrunnelseForSenInnsending) {
            søknadMal.setBegrunnelseForSenInnsending(begrunnelseForSenInnsending);
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

        public SøknadEntitet build() {
            return søknadMal;
        }
    }
}
