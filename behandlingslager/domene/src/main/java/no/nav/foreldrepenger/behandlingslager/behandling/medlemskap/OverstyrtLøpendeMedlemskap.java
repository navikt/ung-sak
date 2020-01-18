package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Embeddable
public class OverstyrtLøpendeMedlemskap {

    @ChangeTracked
    @Column(name = "overstyringsdato")
    private LocalDate overstyringsdato;

    @Convert(converter = Utfall.KodeverdiConverter.class)
    @Column(name="overstyrt_utfall", nullable = false)
    private Utfall vilkårUtfall = Utfall.UDEFINERT;

    @ChangeTracked
    @Convert(converter = Avslagsårsak.KodeverdiConverter.class)
    @Column(name="avslagsarsak", nullable = false)
    private Avslagsårsak avslagsårsak = Avslagsårsak.UDEFINERT;

    OverstyrtLøpendeMedlemskap() {
        //hibernate
    }

    public OverstyrtLøpendeMedlemskap(LocalDate overstyringsdato, Utfall vilkårUtfall, Avslagsårsak avslagsårsak) {
        this.overstyringsdato = overstyringsdato;
        this.vilkårUtfall = vilkårUtfall;
        this.avslagsårsak = avslagsårsak;
    }

    public Optional<LocalDate> getOverstyringsdato() {
        return Optional.ofNullable(overstyringsdato);
    }

    public Utfall getVilkårUtfall() {
        return vilkårUtfall;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
