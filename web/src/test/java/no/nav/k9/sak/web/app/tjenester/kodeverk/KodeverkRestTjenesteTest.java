package no.nav.k9.sak.web.app.tjenester.kodeverk;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.web.app.tjenester.kodeverk.dto.AlleKodeverdierSomObjektResponse;
import no.nav.k9.sak.web.app.tjenester.kodeverk.dto.KodeverdiSomObjekt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.SortedSet;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class KodeverkRestTjenesteTest {

    @Inject
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private static <K extends Kodeverdi> void checkResponseSet(final SortedSet<KodeverdiSomObjekt<K>> responseSet, final java.util.Set<K> statiskSet) {
        assertThat(responseSet.stream().map(ko -> ko.getMadeFrom()).toList()).containsExactlyInAnyOrderElementsOf(statiskSet);
    }

    @Test
    public void skal_hente_statiske_kodeverdier() {
        final KodeverkRestTjeneste tjeneste = new KodeverkRestTjeneste(behandlendeEnhetTjeneste);
        final AlleKodeverdierSomObjektResponse response = tjeneste.alleKodeverdierSomObjekt();
        final var statiske = StatiskeKodeverdier.alle;
        // Sjekk nokon av verdiane. Kan legge til fleire viss ein ønsker.
        checkResponseSet(response.aktivitetStatuser(), statiske.aktivitetStatuser());
        checkResponseSet(response.arbeidskategorier(), statiske.arbeidskategorier());
        checkResponseSet(response.arbeidsforholdHandlingTyper(), statiske.arbeidsforholdHandlingTyper());
        checkResponseSet(response.avslagsårsaker(), statiske.avslagsårsaker());

        // Sjekk spesialtilfelle i respons, Avslagsprsaker gruppert pr vilkårtype.
        final VilkårType vilkårtype = VilkårType.ALDERSVILKÅR;
        final var k9Vk3Avslagsårsaker = VilkårType.finnAvslagårsakerGruppertPåVilkårType().get(vilkårtype);
        checkResponseSet(response.avslagårsakerPrVilkårTypeKode().get(vilkårtype.getKode()), k9Vk3Avslagsårsaker);

        // Venteårsak er også litt spesiell
        final List<Venteårsak> got = response.venteårsaker().stream().map(ko -> ko.getMadeFrom()).toList();
        final List<Venteårsak> expected = statiske.venteårsaker().stream().toList();
        assertThat(got).containsExactlyInAnyOrderElementsOf(expected);
    }

}
