package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.Periode;

@Entity(name = "SykdomVurderingVersjon")
@Table(name = "SYKDOM_VURDERING_VERSJON")
public class SykdomVurderingVersjon implements Comparable<SykdomVurderingVersjon> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING_VERSJON")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERING_ID", nullable = false, updatable = false) //TODO:modifiers?
    private SykdomVurdering sykdomVurdering;

    @Column(name = "TEKST", nullable = false)
    private String tekst;

    @Column(name = "RESULTAT", nullable = false)
    private Resultat resultat;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @DiffIgnore
    @Column(name = "ENDRET_AV")
    private String endretAv;

    @DiffIgnore
    @Column(name = "ENDRET_TID")
    private LocalDateTime endretTidspunkt; // NOSONAR

    @Column(name = "ENDRET_BEHANDLING_UUID", nullable = false)
    private UUID endretBehandlingUuid;

    @Column(name = "ENDRET_SAKSNUMMER", nullable = false)
    private String endretSaksnummer; //TODO: type?

    @ManyToOne
    @JoinColumn(name = "ENDRET_FOR_PERSON_ID", nullable = false)
    private SykdomPerson endretForPerson;

    @OneToOne(mappedBy = "sykdomVurderingVersjon")
    private SykdomVurderingVersjonBesluttet besluttet;

    @OneToMany
    @JoinTable(
        name="SYKDOM_VURDERING_VERSJON_DOKUMENT",
        joinColumns = @JoinColumn( name="SYKDOM_VURDERING_VERSJON_ID"),
        inverseJoinColumns = @JoinColumn( name="SYKDOM_DOKUMENT_ID")
    )
    private List<SykdomDokument> dokumenter = new ArrayList<>();

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "SYKDOM_VURDERING_VERSJON_ID")
    private List<SykdomVurderingPeriode> perioder = new ArrayList<>();

    SykdomVurderingVersjon() {
        // hibernate
    }

    public SykdomVurderingVersjon(
            SykdomVurdering sykdomVurdering,
            String tekst,
            Resultat resultat,
            Long versjon,
            String endretAv,
            LocalDateTime endretTidspunkt,
            UUID endretBehandlingUuid,
            String endretSaksnummer,
            SykdomPerson endretForPerson,
            SykdomVurderingVersjonBesluttet besluttet,
            List<SykdomDokument> dokumenter,
            List<Periode> perioder) {
        this.sykdomVurdering = sykdomVurdering;
        this.tekst = tekst;
        this.resultat = resultat;
        this.versjon = versjon;
        this.endretAv = endretAv;
        this.endretTidspunkt = endretTidspunkt;
        this.endretBehandlingUuid = endretBehandlingUuid;
        this.endretSaksnummer = endretSaksnummer;
        this.endretForPerson = endretForPerson;
        this.besluttet = besluttet;
        this.dokumenter = dokumenter.stream().collect(Collectors.toList());
        this.perioder = perioder.stream().map(p -> new SykdomVurderingPeriode(this, p.getFom(), p.getTom(), endretAv, endretTidspunkt)).collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public SykdomVurdering getSykdomVurdering() {
        return sykdomVurdering;
    }

    public String getTekst() {
        return tekst;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public Long getVersjon() {
        return versjon;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }

    public UUID getEndretBehandlingUuid() {
        return endretBehandlingUuid;
    }

    public String getEndretSaksnummer() {
        return endretSaksnummer;
    }

    public SykdomPerson getEndretForPerson() {
        return endretForPerson;
    }

    public SykdomVurderingVersjonBesluttet getBesluttet() {
        return besluttet;
    }

    public List<SykdomDokument> getDokumenter() {
        return dokumenter;
    }

    public List<SykdomVurderingPeriode> getPerioder() {
        return perioder;
    }

    @Override
    public int compareTo(SykdomVurderingVersjon v2) {
        return getVersjon().compareTo(v2.getVersjon());
    }
}
