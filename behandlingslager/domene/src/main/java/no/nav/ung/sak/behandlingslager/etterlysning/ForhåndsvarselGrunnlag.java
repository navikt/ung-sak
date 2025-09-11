package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.Immutable;

import java.util.*;

public class ForhåndsvarselGrunnlag extends BaseEntitet {

    private Long id;

    private Forhåndsvarsler forhåndsvarsler;

    private UUID grunnlagsreferanse;

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
