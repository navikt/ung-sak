package no.nav.k9.sak.behandlingslager.behandling.søknad;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingslager.kodeverk.RelasjonsRolleTypeKodeverdiConverter;
import no.nav.k9.sak.typer.AktørId;

/** Mapper opp personer angitt av søker i søknad. */
@Entity(name = "SøknadAngittPerson")
@Table(name = "SO_SOEKNAD_ANGITT_PERSON")
public class SøknadAngittPersonEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOEKNAD_ANGITT_PERSON")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    /** Angitt navn ( kun hvis ikke bruker er identifisert. */
    @Column(name = "navn")
    private String navn;

    /** Angitt fødselsdato (kun hvis ikke person er identifisert. */
    @Column(name = "foedselsdato")
    private LocalDate fødselsdato;

    /** Angitt aktørid hvis bruker er identifisert. */
    @Column(name = "aktoer_id")
    private AktørId aktørId;

    @Convert(converter = RelasjonsRolleTypeKodeverdiConverter.class)
    @Column(name = "rolle", nullable = false)
    private RelasjonsRolleType rolle = RelasjonsRolleType.UDEFINERT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "soeknad_id", nullable = false, updatable = false)
    private SøknadEntitet søknad;

    /** Ekstra opplysninger, beskrivelser knyttet til hvorfor de er angitt i søknaden. */
    @Column(name = "tilleggsopplysninger")
    private String tilleggsopplysninger;

    /** Kodeverdi for situasjon, angis dynamisk for forholdet. */
    @Column(name = "situasjon_kode")
    private String situasjonKode;

    SøknadAngittPersonEntitet() {
        // for jpa
    }

    public SøknadAngittPersonEntitet(String navn, LocalDate fødselsdato, RelasjonsRolleType rolle) {
        this.navn = navn;
        this.fødselsdato = fødselsdato;
        this.rolle = rolle;
    }

    public SøknadAngittPersonEntitet(AktørId aktørId, RelasjonsRolleType rolle) {
        this.aktørId = aktørId;
        this.rolle = rolle;
    }

    public SøknadAngittPersonEntitet(String navn, LocalDate fødselsdato, RelasjonsRolleType rolle, String situasjonKode, String tilleggsopplysninger) {
        this.navn = navn;
        this.fødselsdato = fødselsdato;
        this.rolle = rolle;
        this.situasjonKode = situasjonKode;
        this.tilleggsopplysninger = tilleggsopplysninger;
    }

    public SøknadAngittPersonEntitet(AktørId aktørId, RelasjonsRolleType rolle, String situasjonKode, String tilleggsopplysninger) {
        this.aktørId = aktørId;
        this.rolle = rolle;
        this.situasjonKode = situasjonKode;
        this.tilleggsopplysninger = tilleggsopplysninger;
    }

    // copy ctor
    SøknadAngittPersonEntitet(SøknadAngittPersonEntitet angitt) {
        this.aktørId = angitt.aktørId;
        this.navn = angitt.navn;
        this.fødselsdato = angitt.fødselsdato;
        this.rolle = angitt.rolle;
    }

    void setSøknad(SøknadEntitet søknad) {
        if (this.søknad != null) {
            if (!Objects.equals(søknad, this.søknad)) {
                throw new IllegalArgumentException("Kan ikke reassigne denne fra " + this.søknad + " til " + søknad);
            }
        }
        this.søknad = søknad;
    }

    public Long getId() {
        return id;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public RelasjonsRolleType getRolle() {
        return rolle;
    }

    public String getTilleggsopplysninger() {
        return tilleggsopplysninger;
    }

    public String getSituasjonKode() {
        return situasjonKode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof SøknadAngittPersonEntitet)) {
            return false;
        }
        var other = (SøknadAngittPersonEntitet) obj;
        return Objects.equals(this.fødselsdato, other.fødselsdato)
            && Objects.equals(this.rolle, other.rolle)
            && Objects.equals(this.aktørId, other.aktørId)
            && Objects.equals(this.navn, other.navn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, rolle, fødselsdato, navn);
    }

}
