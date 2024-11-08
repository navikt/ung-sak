package no.nav.k9.sak.behandlingslager.behandling.personopplysning;

import java.util.Objects;

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

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.behandlingslager.kodeverk.RelasjonsRolleTypeKodeverdiConverter;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "PersonopplysningRelasjon")
@Table(name = "PO_RELASJON")
@DynamicInsert
@DynamicUpdate
public class PersonRelasjonEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_RELASJON")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "fra_aktoer_id", updatable = false, nullable = false)))
    @ChangeTracked
    private AktørId fraAktørId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "til_aktoer_id", updatable = false, nullable = false)))
    @ChangeTracked
    private AktørId tilAktørId;

    @Convert(converter = RelasjonsRolleTypeKodeverdiConverter.class)
    @Column(name = "relasjonsrolle", nullable = false)
    @ChangeTracked
    private RelasjonsRolleType relasjonsrolle;

    @Column(name = "har_samme_bosted")
    @ChangeTracked
    private Boolean harSammeBosted = Boolean.FALSE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    PersonRelasjonEntitet() {
    }

    PersonRelasjonEntitet(PersonRelasjonEntitet relasjon) {
        this.fraAktørId = relasjon.getAktørId();
        this.tilAktørId = relasjon.getTilAktørId();
        this.relasjonsrolle = relasjon.getRelasjonsrolle();
        this.harSammeBosted = relasjon.getHarSammeBosted();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { fraAktørId, this.relasjonsrolle, this.tilAktørId };
        return IndexKeyComposer.createKey(keyParts);
    }

    void setFraAktørId(AktørId fraAktørId) {
        this.fraAktørId = fraAktørId;
    }

    void setTilAktørId(AktørId tilAktørId) {
        this.tilAktørId = tilAktørId;
    }

    void setHarSammeBosted(Boolean harSammeBosted) {
        this.harSammeBosted = harSammeBosted;
    }

    void setRelasjonsrolle(RelasjonsRolleType relasjonsrolle) {
        this.relasjonsrolle = relasjonsrolle;
    }

    void setPersonopplysningInformasjon(PersonInformasjonEntitet personopplysningInformasjon) {
        this.personopplysningInformasjon = personopplysningInformasjon;
    }

    @Override
    public AktørId getAktørId() {
        return fraAktørId;
    }

    public AktørId getTilAktørId() {
        return tilAktørId;
    }

    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }

    public Boolean getHarSammeBosted() {
        return harSammeBosted;
    }

    @Override
    public String toString() {
        // tar ikke med til/fra Aktør da det fort lekker sensitive opplysninger i logger
        var sb = new StringBuilder("PersonRelasjonEntitet<");
        sb.append("relasjonsrolle=").append(relasjonsrolle);
        sb.append('>');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonRelasjonEntitet entitet = (PersonRelasjonEntitet) o;
        return Objects.equals(fraAktørId, entitet.fraAktørId) &&
            Objects.equals(tilAktørId, entitet.tilAktørId) &&
            Objects.equals(harSammeBosted, entitet.harSammeBosted) &&
            Objects.equals(relasjonsrolle, entitet.relasjonsrolle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fraAktørId, tilAktørId, harSammeBosted, relasjonsrolle);
    }

}
