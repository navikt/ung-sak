package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.Immutable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "UttalelseGrunnlag")
@Table(name = "GR_UTTALELSE")
public class UttalelseGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_UTTALELSE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @ChangeTracked
    @JoinColumn(name = "uttalelse_id", nullable = false)
    private Uttalelser uttalelser;

    @Column(name = "grunnlagsreferanse", updatable = false, unique = true)
    private UUID grunnlagsreferanse;

    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    public UttalelseGrunnlag() {
    }

    public UttalelseGrunnlag(Long behandlingId) {
        this.id = behandlingId;
        this.grunnlagsreferanse = UUID.randomUUID();
    }

    public UttalelseGrunnlag(Long behandlingId, UttalelseGrunnlag grunnlag) {
        this.id = behandlingId;
        this.uttalelser = grunnlag.uttalelser;
        this.grunnlagsreferanse = UUID.randomUUID();
    }

    public Uttalelser getUttalelser() { return uttalelser;}

    void leggTilUttalelser(Collection<UttalelseV2> uttalelser) {
        var varsler = this.uttalelser !=null ? new HashSet<>(this.uttalelser.getUttalelser()) : new HashSet<UttalelseV2>(Set.of());
        varsler.addAll(uttalelser);
        this.uttalelser = new Uttalelser(varsler);
    }
}
