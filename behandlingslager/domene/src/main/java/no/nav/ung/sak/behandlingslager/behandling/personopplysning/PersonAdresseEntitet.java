package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import java.util.Objects;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.geografisk.AdresseType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

@Entity(name = "PersonopplysningAdresse")
@Table(name = "PO_ADRESSE")
@DynamicInsert
@DynamicUpdate
public class PersonAdresseEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_ADRESSE")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @Embedded
    private DatoIntervallEntitet periode;

    @Convert(converter = AdresseTypeKodeverdiConverter.class)
    @Column(name = "adresse_type", nullable = false)
    private AdresseType adresseType;

    @ChangeTracked
    @Column(name = "adresselinje1")
    private String adresselinje1;

    @ChangeTracked
    @Column(name = "adresselinje2")
    private String adresselinje2;

    @ChangeTracked
    @Column(name = "adresselinje3")
    private String adresselinje3;

    @ChangeTracked
    @Column(name = "adresselinje4")
    private String adresselinje4;

    @ChangeTracked
    @Column(name = "postnummer")
    private String postnummer;

    @ChangeTracked
    @Column(name = "poststed")
    private String poststed;

    @ChangeTracked
    @Column(name = "land")
    private String land;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    PersonAdresseEntitet() {
    }

    PersonAdresseEntitet(PersonAdresseEntitet adresse) {
        this.adresselinje1 = adresse.getAdresselinje1();
        this.adresselinje2 = adresse.getAdresselinje2();
        this.adresselinje3 = adresse.getAdresselinje3();
        this.adresselinje4 = adresse.getAdresselinje4();
        this.adresseType = adresse.getAdresseType();
        this.postnummer = adresse.getPostnummer();
        this.poststed = adresse.getPoststed();
        this.land = adresse.getLand();

        this.aktørId = adresse.getAktørId();
        this.periode = adresse.getPeriode();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { aktørId, adresseType, land, periode };
        return IndexKeyComposer.createKey(keyParts);
    }

    void setPersonopplysningInformasjon(PersonInformasjonEntitet personopplysningInformasjon) {
        this.personopplysningInformasjon = personopplysningInformasjon;
    }

    public AdresseType getAdresseType() {
        return adresseType;
    }

    void setAdresseType(AdresseType adresseType) {
        this.adresseType = adresseType;
    }

    public String getAdresselinje1() {
        return adresselinje1;
    }

    void setAdresselinje1(String adresselinje) {
        this.adresselinje1 = adresselinje == null ? null : adresselinje.trim();
    }

    public String getAdresselinje2() {
        return adresselinje2;
    }

    void setAdresselinje2(String adresselinje) {
        this.adresselinje2 = adresselinje == null ? null : adresselinje.trim();
    }

    public String getAdresselinje3() {
        return adresselinje3;
    }

    void setAdresselinje3(String adresselinje) {
        this.adresselinje3 = adresselinje == null ? null : adresselinje.trim();
    }

    public String getAdresselinje4() {
        return adresselinje4;
    }

    void setAdresselinje4(String adresselinje) {
        this.adresselinje4 = adresselinje == null ? null : adresselinje.trim();
    }

    public String getPostnummer() {
        return postnummer;
    }

    void setPostnummer(String postnummer) {
        this.postnummer = postnummer;
    }

    public String getPoststed() {
        return poststed;
    }

    void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public String getLand() {
        return land;
    }

    void setLand(String land) {
        this.land = land;
    }

    @Override
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonAdresseEntitet entitet = (PersonAdresseEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
            Objects.equals(periode, entitet.periode) &&
            Objects.equals(adresseType, entitet.adresseType) &&
            Objects.equals(land, entitet.land);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, adresseType, land);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "<");
        sb.append("id=").append(id);
        sb.append(", periode=").append(periode);
        sb.append(", adresseType=").append(adresseType);
        sb.append(", postnummer='").append(postnummer).append('\'');
        sb.append(", poststed='").append(poststed).append('\'');
        sb.append(", land='").append(land).append('\'');
        sb.append('>');
        return sb.toString();
    }

}
