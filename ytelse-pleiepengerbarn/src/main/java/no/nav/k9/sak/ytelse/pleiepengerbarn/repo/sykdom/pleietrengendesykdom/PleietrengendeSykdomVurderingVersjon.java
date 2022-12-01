package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.behandlingslager.kodeverk.SykdomResultatTypeConverter;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Person;

@Entity(name = "PleietrengendeSykdomVurderingVersjon")
@Table(name = "PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON")
public class PleietrengendeSykdomVurderingVersjon implements Comparable<PleietrengendeSykdomVurderingVersjon> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_VURDERING_ID", nullable = false, updatable = false) //TODO:modifiers?
    private PleietrengendeSykdomVurdering pleietrengendeSykdomVurdering;

    @Column(name = "TEKST", nullable = false)
    private String tekst;

    @Column(name = "RESULTAT", nullable = false)
    @Convert(converter = SykdomResultatTypeConverter.class)
    private Resultat resultat;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @DiffIgnore
    @Column(name = "ENDRET_AV")
    private String endretAv;

    @DiffIgnore
    @Column(name = "ENDRET_TID")
    private LocalDateTime endretTidspunkt; // NOSONAR

    @Column(name = "ENDRET_FOR_SOEKERS_BEHANDLING_UUID", nullable = false)
    private UUID endretForSøkersBehandlingUuid;

    @Column(name = "ENDRET_FOR_SOEKERS_SAKSNUMMER", nullable = false)
    private String endretForSøkersSaksnummer; //TODO: type?

    @ManyToOne
    @JoinColumn(name = "ENDRET_FOR_SOEKER", nullable = false)
    private Person endretForSøker;

    @OneToOne(mappedBy = "pleietrengendeSykdomVurderingVersjon")
    private PleietrengendeSykdomVurderingVersjonBesluttet besluttet;

    @OneToMany
    @JoinTable(
        name="PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ANVENDT_DOKUMENT",
        joinColumns = @JoinColumn( name="PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ID"),
        inverseJoinColumns = @JoinColumn( name="PLEIETRENGENDE_SYKDOM_DOKUMENT_ID")
    )
    private List<PleietrengendeSykdomDokument> dokumenter = new ArrayList<>();

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ID")
    private List<PleietrengendeSykdomVurderingVersjonPeriode> perioder = new ArrayList<>();

    PleietrengendeSykdomVurderingVersjon() {
        // hibernate
    }

    public PleietrengendeSykdomVurderingVersjon(
            PleietrengendeSykdomVurdering pleietrengendeSykdomVurdering,
            String tekst,
            Resultat resultat,
            Long versjon,
            String endretAv,
            LocalDateTime endretTidspunkt,
            UUID endretForSøkersBehandlingUuid,
            String endretForSøkersSaksnummer,
            Person endretForSøker,
            PleietrengendeSykdomVurderingVersjonBesluttet besluttet,
            List<PleietrengendeSykdomDokument> dokumenter,
            List<Periode> perioder) {
        this.pleietrengendeSykdomVurdering = pleietrengendeSykdomVurdering;
        this.tekst = tekst;
        this.resultat = resultat;
        this.versjon = versjon;
        this.endretAv = endretAv;
        this.endretTidspunkt = endretTidspunkt;
        this.endretForSøkersBehandlingUuid = endretForSøkersBehandlingUuid;
        this.endretForSøkersSaksnummer = endretForSøkersSaksnummer;
        this.endretForSøker = endretForSøker;
        this.besluttet = besluttet;
        this.dokumenter = dokumenter.stream().collect(Collectors.toList());
        this.perioder = perioder.stream().map(p -> new PleietrengendeSykdomVurderingVersjonPeriode(this, p.getFom(), p.getTom(), endretAv, endretTidspunkt)).collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public PleietrengendeSykdomVurdering getSykdomVurdering() {
        return pleietrengendeSykdomVurdering;
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

    public UUID getEndretForSøkersBehandlingUuid() {
        return endretForSøkersBehandlingUuid;
    }

    public String getEndretForSøkersSaksnummer() {
        return endretForSøkersSaksnummer;
    }

    public Person getEndretForSøker() {
        return endretForSøker;
    }

    public boolean isBesluttet() {
        return besluttet != null;
    }

    public PleietrengendeSykdomVurderingVersjonBesluttet getBesluttet() {
        return besluttet;
    }

    public void setBesluttet(PleietrengendeSykdomVurderingVersjonBesluttet besluttet) {
        this.besluttet = besluttet;
    }

    public List<PleietrengendeSykdomDokument> getDokumenter() {
        return dokumenter;
    }

    public List<PleietrengendeSykdomVurderingVersjonPeriode> getPerioder() {
        return perioder;
    }

    @Override
    public int compareTo(PleietrengendeSykdomVurderingVersjon v2) {
        return getVersjon().compareTo(v2.getVersjon());
    }
}
