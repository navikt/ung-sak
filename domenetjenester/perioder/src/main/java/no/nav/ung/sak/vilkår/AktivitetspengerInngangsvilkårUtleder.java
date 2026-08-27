package no.nav.ung.sak.vilkår;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.ung.kodeverk.vilkår.VilkårType.*;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD )
public class AktivitetspengerInngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        SØKNADSFRIST,
        ALDERSVILKÅR,
        BOSTEDSVILKÅR,
        ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
        BISTANDSVILKÅR,
        FORUTGÅENDE_MEDLEMSKAPSVILKÅRET
    );

    private static final List<VilkårType> YTELSE_VILKÅR_2 = asList(
        SØKNADSFRIST,
        ALDERSVILKÅR,
        BOSTEDSVILKÅR,
        ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
        BISTANDSVILKÅR,
        AKTIVITETSVILKÅR,
        FORUTGÅENDE_MEDLEMSKAPSVILKÅRET
    );
    private boolean aktivitetsvilkårEnabled;

    public AktivitetspengerInngangsvilkårUtleder() {
        //for CDI proxy
    }

    public AktivitetspengerInngangsvilkårUtleder(@KonfigVerdi(value = "AKTIVITETSVILKAAR_ENABLED", required = false, defaultVerdi = "true") boolean aktivitetsvilkårEnabled) {
        this.aktivitetsvilkårEnabled = aktivitetsvilkårEnabled;
    }


    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        return new UtledeteVilkår(null, aktivitetsvilkårEnabled ? YTELSE_VILKÅR_2 : YTELSE_VILKÅR);
    }

}
