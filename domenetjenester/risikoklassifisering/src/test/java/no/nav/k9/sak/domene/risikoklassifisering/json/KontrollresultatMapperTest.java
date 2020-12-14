package no.nav.k9.sak.domene.risikoklassifisering.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.FaresignalWrapper;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.KontrollresultatWrapper;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.rest.FaresignalerRespons;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.rest.Faresignalgruppe;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.kontroll.kodeverk.KontrollResultatkode;
import no.nav.vedtak.kontroll.v1.KontrollResultatV1;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class KontrollresultatMapperTest {

    @Inject
    KontrollresultatMapper kontrollresultatMapper;

    @Test
    public void skal_teste_at_mapping_av_kontoll_resultat_skjer_korrekt_for_høy_risiko() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act
        KontrollresultatWrapper wrapper = kontrollresultatMapper.fraKontrakt(lagKontrollresultat(uuid, KontrollResultatkode.HØY));

        // Assert
        assertThat(wrapper.getBehandlingUuid()).isEqualTo(uuid);
        assertThat(wrapper.getKontrollresultatkode()).isEqualTo(Kontrollresultat.HØY);
    }

    @Test
    public void skal_teste_at_mapping_av_kontoll_resultat_skjer_korrekt_for_ikke_høy_risiko() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act
        KontrollresultatWrapper wrapper = kontrollresultatMapper.fraKontrakt(lagKontrollresultat(uuid, KontrollResultatkode.IKKE_HØY));

        // Assert
        assertThat(wrapper.getBehandlingUuid()).isEqualTo(uuid);
        assertThat(wrapper.getKontrollresultatkode()).isEqualTo(Kontrollresultat.IKKE_HØY);
    }


    @Test
    public void skal_teste_at_mapping_av_kontoll_resultat_skjer_korrekt_for_ikke_klassifisert() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act
        KontrollresultatWrapper wrapper = kontrollresultatMapper.fraKontrakt(lagKontrollresultat(uuid, KontrollResultatkode.IKKE_KLASSIFISERT));

        // Assert
        assertThat(wrapper.getBehandlingUuid()).isEqualTo(uuid);
        assertThat(wrapper.getKontrollresultatkode()).isEqualTo(Kontrollresultat.IKKE_KLASSIFISERT);
    }

    @Test
    public void skal_gjøre_faresignal_respons_om_til_wrapper() {
        // Arrange
        FaresignalerRespons respons = new FaresignalerRespons();
        List<String> faresignaler = Arrays.asList("Dette er en test", "Dette er også en test", "123 321 987");
        respons.setIayFaresignaler(lagFaresignalgruppe("HOY", faresignaler));
        respons.setMedlFaresignaler(lagFaresignalgruppe("IKKE_HOY", faresignaler));
        respons.setRisikoklasse("HOY");

        // Act
        FaresignalWrapper wrapper = kontrollresultatMapper.fraFaresignalRespons(respons);

        // Assert
        assertThat(wrapper.getKontrollresultat()).isEqualTo(Kontrollresultat.HØY);
        assertThat(wrapper.getIayFaresignaler()).isNotNull();
        assertThat(wrapper.getIayFaresignaler().getFaresignaler()).containsAll(faresignaler);

        assertThat(wrapper.getMedlFaresignaler().getKontrollresultat()).isEqualTo(Kontrollresultat.IKKE_HØY);
        assertThat(wrapper.getMedlFaresignaler().getFaresignaler()).containsAll(faresignaler);
    }


    private KontrollResultatV1 lagKontrollresultat(UUID uuid, KontrollResultatkode resultatkode) {
        KontrollResultatV1.Builder builder = new KontrollResultatV1.Builder();
        return builder.medBehandlingUuid(uuid).medResultatkode(resultatkode).build();
    }

    private Faresignalgruppe lagFaresignalgruppe(String risikoklasse, List<String> faresignaler) {
        Faresignalgruppe gruppe = new Faresignalgruppe();
        gruppe.setFaresignaler(faresignaler);
        gruppe.setRisikoklasse(risikoklasse);
        return gruppe;
    }

}
