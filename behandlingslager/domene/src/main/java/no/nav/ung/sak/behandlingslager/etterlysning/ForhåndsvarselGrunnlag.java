package no.nav.ung.sak.behandlingslager.etterlysning;

import java.util.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.Immutable;

@Entity(name = "ForhåndsvarselGrunnlag")
@Table(name = "FORH_VARSEL_GRUNNLAG")
public class ForhåndsvarselGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FORH_VARSEL_GRUNNLAG")
    private Long id;


    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;


    @ManyToOne
    @Immutable
    @ChangeTracked
    @JoinColumn(name = "forhåndsvarsler_id", nullable = false)
    private Forhåndsvarsler forhåndsvarsler;

    @Column(name = "grunnlagsreferanse", updatable = false, unique = true)
    private UUID grunnlagsreferanse;

    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    public ForhåndsvarselGrunnlag() {
    }

    public ForhåndsvarselGrunnlag(Long behandlingId) {
        this.id = behandlingId;
        this.grunnlagsreferanse = UUID.randomUUID();
    }

    public ForhåndsvarselGrunnlag(Long behandlingId, ForhåndsvarselGrunnlag grunnlag) {
        this.id = behandlingId;
        this.forhåndsvarsler = grunnlag.forhåndsvarsler;
        this.grunnlagsreferanse = UUID.randomUUID();
    }

    public Forhåndsvarsler getForhåndsvarsler() { return forhåndsvarsler;}

    void leggTilForhåndsvarsler(Collection<Etterlysning> forhåndsvarsel) {
        var varsler = this.forhåndsvarsler !=null ? new HashSet<>(this.forhåndsvarsler.getVarsler()) : new HashSet<Etterlysning>(Set.of());
        varsler.addAll(forhåndsvarsel);
        this.forhåndsvarsler = new Forhåndsvarsler(varsler);
    }
}
