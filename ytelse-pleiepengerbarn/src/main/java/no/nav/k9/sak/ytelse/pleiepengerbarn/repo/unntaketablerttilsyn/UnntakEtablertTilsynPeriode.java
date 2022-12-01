package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

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
import jakarta.persistence.Version;
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.behandlingslager.kodeverk.SykdomResultatTypeConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "UnntakEtablertTilsynPeriode")
@Table(name = "psb_unntak_etablert_tilsyn_periode")
public class UnntakEtablertTilsynPeriode extends BaseEntitet implements IndexKey  {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_UNNTAK_ETABLERT_TILSYN_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "psb_unntak_etablert_tilsyn_id", nullable = false, updatable = false, unique = true)
    private UnntakEtablertTilsyn unntakEtablertTilsyn;

    @ChangeTracked
    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Column(name = "resultat", nullable = false)
    @Convert(converter = SykdomResultatTypeConverter.class)
    private Resultat resultat;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "soeker_aktoer_id", unique = true, nullable = false, updatable = false)))
    private AktørId aktørId;

    @Column(name = "kilde_behandling_id", nullable = false, updatable = false)
    private Long kildeBehandlingId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    public UnntakEtablertTilsynPeriode() {
        // hibernate
    }

    public UnntakEtablertTilsynPeriode(UnntakEtablertTilsynPeriode that) {
        this.begrunnelse = that.begrunnelse;
        this.resultat = that.resultat;
        this.periode = that.periode;
        this.kildeBehandlingId = that.kildeBehandlingId;
        this.aktørId = that.aktørId;
    }

    public UnntakEtablertTilsynPeriode(DatoIntervallEntitet periode, String begrunnelse, Resultat resultat, AktørId aktørId, Long kildeBehandlingId) {
        this.periode = periode;
        this.begrunnelse = begrunnelse;
        this.resultat = resultat;
        this.aktørId = aktørId;
        this.kildeBehandlingId = kildeBehandlingId;
    }

    public Long getId() {
        return id;
    }

    public UnntakEtablertTilsyn getUnntakEtablertTilsyn() {
        return unntakEtablertTilsyn;
    }

    public UnntakEtablertTilsynPeriode medUnntakEtablertTilsyn(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        this.unntakEtablertTilsyn = unntakEtablertTilsyn;
        return this;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public Long getKildeBehandlingId() {
        return kildeBehandlingId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public UnntakEtablertTilsynPeriode medPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
        return this;
    }

    public UnntakEtablertTilsynPeriode medBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
        return this;
    }

    public UnntakEtablertTilsynPeriode medResultat(Resultat resultat) {
        this.resultat = resultat;
        return this;
    }

    public UnntakEtablertTilsynPeriode medKildeBehandlingId(Long kildeBehandlingId) {
        this.kildeBehandlingId = kildeBehandlingId;
        return this;
    }

    public UnntakEtablertTilsynPeriode medAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnntakEtablertTilsynPeriode that = (UnntakEtablertTilsynPeriode) o;
        return periode.equals(that.periode) && unntakEtablertTilsyn.equals(that.unntakEtablertTilsyn) && begrunnelse.equals(that.begrunnelse) && resultat == that.resultat && kildeBehandlingId.equals(that.kildeBehandlingId) && aktørId.equals(that.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, unntakEtablertTilsyn, begrunnelse, resultat, kildeBehandlingId, aktørId);
    }

    @Override
    public String toString() {
        return "UnntakEtablertTilsynPeriode{" +
            "id=" + id +
            ", periode=" + periode +
            ", resultat=" + resultat +
            ", versjon=" + versjon +
            ", kildeBehandlingId=" + kildeBehandlingId +
            ", aktørId=" + aktørId +
            '}';
    }
}
