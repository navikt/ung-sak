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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_GRUNNLAG")
    private Long id;

    private SykdomGrunnlag grunnlag;

    private SykdomPerson søker;

    private Saksnummer saksnummer;

    private UUID behandlingUUID;

    private Long behandlingsnummer;

    private Long versjon;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

}
