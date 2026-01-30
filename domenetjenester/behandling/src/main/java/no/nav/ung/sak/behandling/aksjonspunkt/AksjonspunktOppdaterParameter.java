package no.nav.ung.sak.behandling.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.typer.AktørId;

/** Input data til AksjonspunktOppdaterere. */
public final class AksjonspunktOppdaterParameter {
    private final Behandling behandling;
    private final VilkårResultatBuilder vilkårBuilder;
    private final Optional<Aksjonspunkt> aksjonspunkt;
    private BehandlingReferanse ref;
    private final boolean erBegrunnelseEndret;

    private AksjonspunktOppdaterParameter(Behandling behandling, Optional<Aksjonspunkt> aksjonspunkt, VilkårResultatBuilder vilkårBuilder, String begrunnelse) {
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(aksjonspunkt, "Optional<Aksjonspunkt> kan ikke selv være null");
        Objects.requireNonNull(vilkårBuilder, "vilkårBuilder");
        this.behandling = behandling;
        this.vilkårBuilder = vilkårBuilder;
        this.aksjonspunkt = aksjonspunkt;
        this.ref = BehandlingReferanse.fra(behandling);
        this.erBegrunnelseEndret = begrunnelse != null ? aksjonspunkt.map(ap -> !Objects.equals(ap.getBegrunnelse(), begrunnelse)).orElse(Boolean.FALSE) : Boolean.FALSE;
    }

    public AksjonspunktOppdaterParameter(Behandling behandling, Optional<Aksjonspunkt> aksjonspunkt, VilkårResultatBuilder vilkårBuilder, BekreftetAksjonspunktDto dto) {
        this(behandling, aksjonspunkt, vilkårBuilder, dto.getBegrunnelse());
    }

    // Test-only
    public AksjonspunktOppdaterParameter(Behandling behandling, Aksjonspunkt aksjonspunkt, BekreftetAksjonspunktDto dto) {
        this(behandling, Optional.ofNullable(aksjonspunkt), Vilkårene.builder(), dto.getBegrunnelse());

    }

    // Test-only
    public AksjonspunktOppdaterParameter(Behandling behandling, Optional<Aksjonspunkt> aksjonspunkt, BekreftetAksjonspunktDto dto) {
        this(behandling, aksjonspunkt, Vilkårene.builder(), dto.getBegrunnelse());
    }

    // Test-only
    public AksjonspunktOppdaterParameter(Behandling behandling, Aksjonspunkt aksjonspunkt, String begrunnelse) {
        this(behandling, Optional.ofNullable(aksjonspunkt), Vilkårene.builder(), begrunnelse);
    }

    /**
     * @deprecated Bruk {@link #getRef()} i stedet.
     */
    @Deprecated
    public Behandling getBehandling() {
        return behandling;
    }

    public Long getBehandlingId() {
        return ref.getBehandlingId();
    }

    public Optional<Aksjonspunkt> getAksjonspunkt() {
        return aksjonspunkt;
    }

    public BehandlingReferanse getRef() {
        return ref;
    }

    /** Returnerer builder som brukes til å bygge opp vilkår. Er mutable. */
    public VilkårResultatBuilder getVilkårResultatBuilder() {
        Objects.requireNonNull(vilkårBuilder, "this.vilkårBuilder");
        return vilkårBuilder;
    }

    public AktørId getAktørId() {
        return ref.getAktørId();
    }

    public boolean erBegrunnelseEndret() {
        return erBegrunnelseEndret;
    }
}
