package no.nav.ung.sak.behandlingslager.behandling.søknad;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingslager.kodeverk.RelasjonsRolleTypeKodeverdiConverter;
import no.nav.ung.sak.typer.AktørId;

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

    public SøknadAngittPersonEntitet(AktørId aktørId, RelasjonsRolleType rolle) {
        this.aktørId = aktørId;
        this.rolle = rolle;
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
        this.rolle = angitt.rolle;
        this.situasjonKode = angitt.situasjonKode;
        this.tilleggsopplysninger = angitt.tilleggsopplysninger;
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
        return Objects.equals(this.rolle, other.rolle)
            && Objects.equals(this.aktørId, other.aktørId)
        ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, rolle);
    }

}
