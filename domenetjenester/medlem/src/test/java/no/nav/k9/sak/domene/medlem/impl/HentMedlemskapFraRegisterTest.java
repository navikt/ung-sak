package no.nav.k9.sak.domene.medlem.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapKildeType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.sak.domene.medlem.api.Medlemskapsperiode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.medl2.Medlemskapsunntak;
import no.nav.vedtak.felles.integrasjon.medl2.MedlemsunntakRestKlient;

public class HentMedlemskapFraRegisterTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final long MEDL_ID_1 = 2663947L;
    private final MedlemsunntakRestKlient restKlient = mock(MedlemsunntakRestKlient.class);
    private HentMedlemskapFraRegister medlemTjeneste;

    @BeforeEach
    public void before() {
        medlemTjeneste = new HentMedlemskapFraRegister(restKlient);
    }

    @Test
    public void skal_hente_medlemsperioder_og_logge_dem_til_saksopplysningslageret() throws Exception {
        // Arrange
        Medlemskapsunntak unntak = mock(Medlemskapsunntak.class);
        when(unntak.getUnntakId()).thenReturn(MEDL_ID_1);
        var fom = LocalDate.now().minusMonths(9);
        when(unntak.getFraOgMed()).thenReturn(fom);
        var tom = LocalDate.now().plusMonths(1);
        when(unntak.getTilOgMed()).thenReturn(tom);
        when(unntak.getDekning()).thenReturn("Full");
        when(unntak.getLovvalg()).thenReturn("ENDL");
        when(unntak.getLovvalgsland()).thenReturn("UZB");
        when(unntak.getKilde()).thenReturn("AVGSYS");
        var besluttet = LocalDate.now();
        when(unntak.getBesluttet()).thenReturn(besluttet);
        when(unntak.getStudieland()).thenReturn("VUT");
        when(unntak.isMedlem()).thenReturn(true);

        when(restKlient.finnMedlemsunntak(eq(AKTØR_ID.getId()), any(), any())).thenReturn(List.of(unntak));

        // Act
        List<Medlemskapsperiode> medlemskapsperioder = medlemTjeneste.finnMedlemskapPerioder(AKTØR_ID, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Assert
        assertThat(medlemskapsperioder).hasSize(1);

        Medlemskapsperiode medlemskapsperiode1 = new Medlemskapsperiode.Builder()
            .medFom(fom)
            .medTom(tom)
            .medDatoBesluttet(besluttet)
            .medErMedlem(true)
            .medDekning(MedlemskapDekningType.FULL)
            .medLovvalg(MedlemskapType.ENDELIG)
            .medLovvalgsland(Landkoder.fraKode("UZB"))
            .medKilde(MedlemskapKildeType.AVGSYS)
            .medStudieland(Landkoder.fraKode("VUT"))
            .medMedlId(MEDL_ID_1)
            .build();
        assertThat(medlemskapsperioder).contains(medlemskapsperiode1);
    }
}
