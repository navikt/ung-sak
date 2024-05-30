package no.nav.k9.sak.forvaltning;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "DumpFeilIm")
@Table(name = "DUMP_FEIL_IM")
public class DumpFeilIM {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DUMP_FEIL_IM")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ChangeTracked
    @JoinColumn(name = "dump_grunnlag_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<DumpFeilIMVilkårperiode> vilkårperioder;

    @ChangeTracked
    @JoinColumn(name = "dump_grunnlag_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<DumpFeilIMFordelperiode> fordelperioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    public DumpFeilIM(Long behandlingId, Set<DumpFeilIMVilkårperiode> vilkårperioder, Set<DumpFeilIMFordelperiode> fordelperioder) {
        this.behandlingId = behandlingId;
        this.vilkårperioder = vilkårperioder;
        this.fordelperioder = fordelperioder;
    }

    public DumpFeilIM() {
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Set<DatoIntervallEntitet> getFordelperioder() {
        return fordelperioder.stream().map(DumpFeilIMFordelperiode::getPeriode).collect(Collectors.toSet());
    }

    public Set<DatoIntervallEntitet> getVilkårperioder() {
        return vilkårperioder.stream().map(DumpFeilIMVilkårperiode::getPeriode).collect(Collectors.toSet());
    }

    void deaktiver() {
        this.aktiv = false;
    }

}
