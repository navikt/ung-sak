package repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.KontinuerligTilsyn;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "SykdomVurdering")
@Table(name = "SYKDOM_VURDERING")
public class SykdomVurdering {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING")
    private Long id;

    @Column(name = "TYPE", nullable = false)
    private SykdomVurderingType type;

    @Column(name = "RANGERING")
    private Long rangering;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERINGER_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers
    private SykdomVurderinger sykdomVurderinger;

    @OneToOne
    @JoinColumn(name = "SYKDOM_VURDERING_ID", nullable = false)
    private SykdomVurderingVersjon sykdomVurderingVersjon;

    @Version
    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomVurdering() {
        // hibernate
    }

}
