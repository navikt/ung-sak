package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "UnntakEtablertTilsynBeskrivelse")
@Table(name = "UNNTAK_ETABLERT_TILSYN_BESKRIVELSE")
public class UnntakEtablertTilsynBeskrivelse extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNNTAK_ETABLERT_TILSYN_BESKRIVELSE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "FOM", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "TOM", nullable = false))
    })
    private DatoIntervallEntitet periode;


    @ManyToOne
    @JoinColumn(name = "UNNTAK_ETABLERT_TILSYN_ID", nullable = false, updatable = false, unique = true)
    private UnntakEtablertTilsyn unntakEtablertTilsyn;


    @Column(name = "MOTTATT_DATO")
    private LocalDate mottattDato;

    @Column(name = "TEKST")
    private String tekst;


    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "SOEKER_AKTOER_ID", unique = true, nullable = false, updatable = false)))
    private AktørId søker;

    public UnntakEtablertTilsynBeskrivelse() {
        // Hibernate
    }

    public UnntakEtablertTilsynBeskrivelse(DatoIntervallEntitet periode, LocalDate mottattDato, String tekst, AktørId søker) {
        this.periode = periode;
        this.mottattDato = mottattDato;
        this.tekst = tekst;
        this.søker = søker;
    }


    public UnntakEtablertTilsynBeskrivelse(UnntakEtablertTilsynBeskrivelse beskrivelse) {
        this.periode = beskrivelse.periode;
        this.mottattDato = beskrivelse.mottattDato;
        this.tekst = beskrivelse.tekst;
        this.søker = beskrivelse.søker;
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

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(mottattDato);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnntakEtablertTilsynBeskrivelse that = (UnntakEtablertTilsynBeskrivelse) o;
        return periode.equals(that.periode) && unntakEtablertTilsyn.equals(that.unntakEtablertTilsyn) && mottattDato.equals(that.mottattDato) && Objects.equals(tekst, that.tekst) && søker.equals(that.søker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, unntakEtablertTilsyn, mottattDato, tekst, søker);
    }


    @Override
    public String toString() {
        return "UnntakEtablertTilsynBeskrivelse{" +
            "id=" + id +
            ", periode=" + periode +
            ", mottattDato=" + mottattDato +
            ", søker=" + søker +
            '}';
    }
}
