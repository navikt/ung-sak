package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

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
import jakarta.persistence.Transient;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.geografisk.Region;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.behandlingslager.kodeverk.LandkoderKodeverdiConverter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

@Entity(name = "PersonopplysningStatsborgerskap")
@Table(name = "PO_STATSBORGERSKAP")
public class StatsborgerskapEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_STATSBORGERSKAP")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @Embedded
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Convert(converter = LandkoderKodeverdiConverter.class)
    @Column(name="statsborgerskap", nullable = false)
    private Landkoder statsborgerskap = Landkoder.UDEFINERT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    @Transient
    private Region region;

    StatsborgerskapEntitet() {
    }

    StatsborgerskapEntitet(StatsborgerskapEntitet statsborgerskap) {
        this.aktørId = statsborgerskap.getAktørId();
        this.periode = statsborgerskap.getPeriode();
        this.statsborgerskap = statsborgerskap.getStatsborgerskap();
    }


    @Override
    public String getIndexKey() {
        Object[] keyParts = { aktørId, statsborgerskap, periode };
        return IndexKeyComposer.createKey(keyParts);
    }

    void setPersonopplysningInformasjon(PersonInformasjonEntitet personopplysningInformasjon) {
        this.personopplysningInformasjon = personopplysningInformasjon;
    }

    void setRegion(Region region) {
        this.region = region;
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

    void setPeriode(DatoIntervallEntitet gyldighetsperiode) {
        this.periode = gyldighetsperiode;
    }


    public Landkoder getStatsborgerskap() {
        return statsborgerskap;
    }


    public Region getRegion() {
        return region;
    }

    void setStatsborgerskap(Landkoder statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatsborgerskapEntitet entitet = (StatsborgerskapEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
                Objects.equals(periode, entitet.periode) &&
                Objects.equals(statsborgerskap, entitet.statsborgerskap);
    }


    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, statsborgerskap);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StatsborgerskapEntitet{");
        sb.append("aktørId=").append(aktørId);
        sb.append(", periode=").append(periode);
        sb.append(", statsborgerskap=").append(statsborgerskap);
        sb.append('}');
        return sb.toString();
    }

}
