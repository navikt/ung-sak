package repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.Saksnummer;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "SykdomVurderingVersjon")
@Table(name = "SYKDOM_VURDERING_VERSJON")
public class SykdomVurderingVersjon {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING_VERSJON")
    private Long id;

    @OneToOne
    @Column(name = "SYKDOM_VURDERING_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers?
    private Long sykdomVurderingId;

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
    private List<SykdomPeriode> perioder = new ArrayList<>();

    SykdomVurderingVersjon() {
        // hibernate
    }

}
