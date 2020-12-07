package repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.JournalpostId;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "SykdomGrunnlag")
@Table(name = "SYKDOM_GRUNNLAG")
public class SykdomGrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_GRUNNLAG")
    private Long id;

    @Column(name = "SYKDOM_GRUNNLAG_UUID", nullable = false)
    private UUID sykdomGrunnlagUUID;

    @OneToMany(mappedBy = "SYKDOM_GRUNNLAG_ID")
    private List<SykdomSøktPeriode> søktePerioder = new ArrayList<>();

    @OneToMany(mappedBy = "SYKDOM_GRUNNLAG_ID")
    private List<SykdomRevurderingPeriode> revurderingPerioder = new ArrayList<>();

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

}
