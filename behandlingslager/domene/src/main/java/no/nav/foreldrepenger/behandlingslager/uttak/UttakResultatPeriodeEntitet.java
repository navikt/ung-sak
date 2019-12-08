package no.nav.foreldrepenger.behandlingslager.uttak;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;

@Entity
@Table(name = "UTTAK_RESULTAT_PERIODE")
public class UttakResultatPeriodeEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_RESULTAT_PERIODE")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uttak_resultat_perioder_id", nullable = false, updatable = false, unique = true)
    private UttakResultatPerioderEntitet perioder;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
            @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet tidsperiode;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Convert(converter = PeriodeResultatType.KodeverdiConverter.class)
    @Column(name="periode_resultat_type", nullable = false)
    private PeriodeResultatType periodeResultatType;

    @Column(name = "kl_periode_resultat_aarsak", nullable = false)
    private String klPeriodeResultatÅrsak = PeriodeResultatÅrsak.UKJENT.getKode();

    @Column(name="PERIODE_RESULTAT_AARSAK", nullable = false)
    private String periodeResultatÅrsak;

    @Override
    public String toString() {
        return "UttakResultatPeriodeEntitet{" +
            "tidsperiode=" + tidsperiode +
            ", periodeResultatType=" + periodeResultatType.getKode() +
            ", periodeResultatÅrsak=" + periodeResultatÅrsak +
            '}';
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFom() {
        return tidsperiode.getFomDato();
    }

    public LocalDate getTom() {
        return tidsperiode.getTomDato();
    }

    public DatoIntervallEntitet getTidsperiode() {
        return tidsperiode;
    }

    public PeriodeResultatType getPeriodeResultatType() {
        return periodeResultatType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public PeriodeResultatÅrsak getPeriodeResultatÅrsak() {
        if (Objects.equals(klPeriodeResultatÅrsak, IkkeOppfyltÅrsak.KODEVERK)) {
            return IkkeOppfyltÅrsak.fraKode(periodeResultatÅrsak);
        } else if (Objects.equals(klPeriodeResultatÅrsak, InnvilgetÅrsak.KODEVERK)) {
            return InnvilgetÅrsak.fraKode(periodeResultatÅrsak);
        }
        return PeriodeResultatÅrsak.UKJENT;
    }

    public void setPerioder(UttakResultatPerioderEntitet perioder) {
        this.perioder = perioder;
    }

    public boolean overlapper(LocalDate dato) {
        Objects.requireNonNull(dato);
        return !dato.isBefore(getFom()) && !dato.isAfter(getTom());
    }

    public boolean isInnvilget() {
        return Objects.equals(getPeriodeResultatType(), PeriodeResultatType.INNVILGET);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UttakResultatPeriodeEntitet that = (UttakResultatPeriodeEntitet) o;
        return Objects.equals(perioder, that.perioder) &&
            Objects.equals(tidsperiode, that.tidsperiode);
    }

    @Override
    public int hashCode() {

        return Objects.hash(perioder, tidsperiode);
    }

    public static class Builder {
        private UttakResultatPeriodeEntitet kladd;

        public Builder(LocalDate fom, LocalDate tom) {
            this.kladd = new UttakResultatPeriodeEntitet();
            this.kladd.tidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        }

        public Builder medPeriodeResultat(PeriodeResultatType periodeResultatType, PeriodeResultatÅrsak periodeResultatÅrsak) {
            kladd.periodeResultatType = periodeResultatType;
            kladd.periodeResultatÅrsak = periodeResultatÅrsak.getKode();
            kladd.klPeriodeResultatÅrsak = periodeResultatÅrsak.getKodeverk();
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public UttakResultatPeriodeEntitet build() {
            Objects.requireNonNull(kladd.tidsperiode, "tidsperiode");
            Objects.requireNonNull(kladd.periodeResultatType, "periodeResultatType");
            return kladd;
        }

    }
}
