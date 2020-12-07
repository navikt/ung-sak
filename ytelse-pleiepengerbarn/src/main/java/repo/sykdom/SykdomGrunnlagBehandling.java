package repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.Saksnummer;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "SykdomGrunnlagBehandling")
@Table(name = "SYKDOM_GRUNNLAG_BEHANDLING")
public class SykdomGrunnlagBehandling {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_GRUNNLAG_BEHANDLING")
    private Long id;

    @OneToOne
    @JoinColumn(name = "SYKDOM_GRUNNLAG_ID", nullable = false)
    private SykdomGrunnlag grunnlag;

    @ManyToOne
    @JoinColumn(name = "SOEKER_PERSON_ID", nullable = false)
    private SykdomPerson søker;

    @Column(name = "SAKSNUMMER", nullable = false)
    private Saksnummer saksnummer;

    @Column(name = "BEHANDLING_UUID", nullable = false)
    private UUID behandlingUUID;

    @Column(name = "BEHANDLINGSNUMMER", nullable = false)
    private Long behandlingsnummer;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

}
