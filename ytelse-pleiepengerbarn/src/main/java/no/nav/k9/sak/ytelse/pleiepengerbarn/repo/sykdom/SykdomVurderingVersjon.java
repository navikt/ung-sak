package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.Saksnummer;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "SykdomVurderingVersjon")
@Table(name = "SYKDOM_VURDERING_VERSJON")
public class SykdomVurderingVersjon {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING_VERSJON")
    private Long id;

    @OneToOne
    @Column(name = "SYKDOM_VURDERING_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers?
    private SykdomVurdering sykdomVurdering;

    @Column(name = "TEKST", nullable = false)
    private String tekst;

    @Column(name = "RESULTAT", nullable = false)
    private Resultat resultat;

    @Version //TODO: ?
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
    private Saksnummer endretSaksnummer; //TODO: type?

    @ManyToOne
    @JoinColumn(name = "ENDRET_FOR_PERSON_ID", nullable = false)
    private SykdomPerson endretForPerson;

    @OneToOne
    @JoinColumn(name = "SYKDOM_VURDERING_VERSJON_ID")
    private SykdomVurderingVersjonBesluttet besluttet;

    @OneToMany
    @JoinTable(
        name="SYKDOM_VURDERING_VERSJON_DOKUMENT",
        joinColumns = @JoinColumn( name="SYKDOM_VURDERING_VERSJON_ID"),
        inverseJoinColumns = @JoinColumn( name="SYKDOM_DOKUMENT_ID")
    )
    private List<SykdomDokument> dokumenter = new ArrayList<>();

    @OneToMany
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
            Saksnummer endretSaksnummer,
            SykdomPerson endretForPerson,
            SykdomVurderingVersjonBesluttet besluttet,
            List<SykdomDokument> dokumenter,
            List<SykdomVurderingPeriode> perioder) {
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
        this.perioder = perioder.stream().collect(Collectors.toList());
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

    public Saksnummer getEndretSaksnummer() {
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
}
