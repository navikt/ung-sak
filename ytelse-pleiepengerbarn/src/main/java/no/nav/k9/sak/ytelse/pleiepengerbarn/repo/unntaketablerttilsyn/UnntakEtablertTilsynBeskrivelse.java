package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "UnntakEtablertTilsynBeskrivelse")
@Table(name = "psb_unntak_etablert_tilsyn_beskrivelse")
public class UnntakEtablertTilsynBeskrivelse extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_UNNTAK_ETABLERT_TILSYN_BESKRIVELSE")
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


    @Column(name = "mottatt_dato")
    private LocalDate mottattDato;

    @Column(name = "tekst")
    private String tekst;


    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "soeker_aktoer_id", unique = true, nullable = false, updatable = false)))
    private AktørId søker;

    @Column(name = "kilde_behandling_id", nullable = false, updatable = false, unique = true)
    private Long kildeBehandlingId;

    public UnntakEtablertTilsynBeskrivelse() {
        // Hibernate
    }

    public UnntakEtablertTilsynBeskrivelse(DatoIntervallEntitet periode, LocalDate mottattDato, String tekst, AktørId søker, Long kildeBehandlingId) {
        this.periode = periode;
        this.mottattDato = mottattDato;
        this.tekst = tekst;
        this.søker = søker;
        this.kildeBehandlingId = kildeBehandlingId;
    }

    UnntakEtablertTilsynBeskrivelse(UnntakEtablertTilsynBeskrivelse beskrivelse) {
        this.periode = beskrivelse.periode;
        this.mottattDato = beskrivelse.mottattDato;
        this.tekst = beskrivelse.tekst;
        this.søker = beskrivelse.søker;
        this.kildeBehandlingId = beskrivelse.kildeBehandlingId;
    }

    public Long getId() {
        return id;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public String getTekst() {
        return tekst;
    }

    public AktørId getSøker() {
        return søker;
    }

    public UnntakEtablertTilsyn getUnntakEtablertTilsyn() {
        return unntakEtablertTilsyn;
    }

    public Long getKildeBehandlingId() {
        return kildeBehandlingId;
    }

    UnntakEtablertTilsynBeskrivelse medUnntakEtablertTilsyn(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        this.unntakEtablertTilsyn = unntakEtablertTilsyn;
        return this;
    }

    UnntakEtablertTilsynBeskrivelse medMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
        return this;
    }

    UnntakEtablertTilsynBeskrivelse medTekst(String tekst) {
        this.tekst = tekst;
        return this;
    }

    UnntakEtablertTilsynBeskrivelse medSøker(AktørId søker) {
        this.søker = søker;
        return this;
    }

    UnntakEtablertTilsynBeskrivelse medPeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return this;
    }

    public UnntakEtablertTilsynBeskrivelse medKildeBehandlingId(Long kildeBehandlingId) {
        this.kildeBehandlingId = kildeBehandlingId;
        return this;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(mottattDato);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnntakEtablertTilsynBeskrivelse that = (UnntakEtablertTilsynBeskrivelse) o;
        return periode.equals(that.periode) && mottattDato.equals(that.mottattDato) && Objects.equals(tekst, that.tekst) && søker.equals(that.søker) && kildeBehandlingId.equals(that.kildeBehandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, mottattDato, tekst, søker, kildeBehandlingId);
    }


    @Override
    public String toString() {
        return "UnntakEtablertTilsynBeskrivelse{" +
            "id=" + id +
            ", periode=" + periode +
            ", mottattDato=" + mottattDato +
            ", søker=" + søker +
            ", kildeBehandlingId=" + kildeBehandlingId +
            '}';
    }

    void setUnntakEtablertTilsyn(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        this.unntakEtablertTilsyn = unntakEtablertTilsyn;
    }
}
