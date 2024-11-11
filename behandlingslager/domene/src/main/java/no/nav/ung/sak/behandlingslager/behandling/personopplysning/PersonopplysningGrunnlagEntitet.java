package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;

import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "PersonopplysningGrunnlagEntitet")
@Table(name = "GR_PERSONOPPLYSNING")
@DynamicUpdate
public class PersonopplysningGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_PERSONOPPLYSNING")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "registrert_informasjon_id", updatable = false)
    private PersonInformasjonEntitet registrertePersonopplysninger;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "overstyrt_informasjon_id", updatable = false)
    private PersonInformasjonEntitet overstyrtePersonopplysninger;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    PersonopplysningGrunnlagEntitet() {
    }

    PersonopplysningGrunnlagEntitet(PersonopplysningGrunnlagEntitet behandlingsgrunnlag) {
        if (behandlingsgrunnlag.getOverstyrtVersjon().isPresent()) {
            this.overstyrtePersonopplysninger = behandlingsgrunnlag.getOverstyrtVersjon().get();
        }
        if (behandlingsgrunnlag.getRegisterVersjon().isPresent()) {
            this.registrertePersonopplysninger = behandlingsgrunnlag.getRegisterVersjon().get();
        }
    }

    /**
     * Kun synlig for abstract test scenario
     * @return id
     */

    public Long getId() {
        return id;
    }

    void setAktiv(final boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setRegistrertePersonopplysninger(PersonInformasjonEntitet registrertePersonopplysninger) {
        this.registrertePersonopplysninger = registrertePersonopplysninger;
    }

    void setOverstyrtePersonopplysninger(PersonInformasjonEntitet overstyrtePersonopplysninger) {
        this.overstyrtePersonopplysninger = overstyrtePersonopplysninger;
    }


    public PersonInformasjonEntitet getGjeldendeVersjon() {
        if (getOverstyrtVersjon().isPresent()) {
            return getOverstyrtVersjon().get();
        }
        return registrertePersonopplysninger;
    }


    public Optional<PersonInformasjonEntitet> getRegisterVersjon() {
        return Optional.ofNullable(registrertePersonopplysninger);
    }


    public Optional<PersonInformasjonEntitet> getOverstyrtVersjon() {
        return Optional.ofNullable(overstyrtePersonopplysninger);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonopplysningGrunnlagEntitet that = (PersonopplysningGrunnlagEntitet) o;
        return Objects.equals(behandlingId, that.behandlingId) &&
                Objects.equals(registrertePersonopplysninger, that.registrertePersonopplysninger) &&
                Objects.equals(overstyrtePersonopplysninger, that.overstyrtePersonopplysninger);
    }


    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, registrertePersonopplysninger, overstyrtePersonopplysninger);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PersonopplysningGrunnlagEntitet{");
        sb.append("id=").append(id);
        sb.append(", aktiv=").append(aktiv);
        sb.append(", registrertePersonopplysninger=").append(registrertePersonopplysninger);
        sb.append(", overstyrtePersonopplysninger=").append(overstyrtePersonopplysninger);
        sb.append('}');
        return sb.toString();
    }
}
