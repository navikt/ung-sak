package no.nav.k9.sak.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.kodeverk.AvslagsårsakKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.UtfallKodeverdiConverter;

@Embeddable
public class OverstyrtLøpendeMedlemskap {

    @ChangeTracked
    @Column(name = "overstyringsdato")
    private LocalDate overstyringsdato;

    @Convert(converter = UtfallKodeverdiConverter.class)
    @Column(name="overstyrt_utfall", nullable = false)
    private Utfall vilkårUtfall = Utfall.UDEFINERT;

    @ChangeTracked
    @Convert(converter = AvslagsårsakKodeverdiConverter.class)
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
