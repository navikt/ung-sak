package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

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

    @OneToMany(mappedBy = "sykdomGrunnlag")
    private List<SykdomSøktPeriode> søktePerioder = new ArrayList<>();

    @OneToMany(mappedBy = "sykdomGrunnlag")
    private List<SykdomRevurderingPeriode> revurderingPerioder = new ArrayList<>();

    @OneToMany()
    @JoinTable(
        name="SYKDOM_GRUNNLAG_VURDERING",
        joinColumns = @JoinColumn( name="SYKDOM_GRUNNLAG_ID"),
        inverseJoinColumns = @JoinColumn( name="SYKDOM_VURDERING_VERSJON_ID")
    )
    private List<SykdomVurderingVersjon> vurderinger = new ArrayList<>();

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

}
