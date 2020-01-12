package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.FastsettBeregningsgrunnlagATFLHistorikkTjeneste;

public class FastsettBeregningsgrunnlagATFLOppdatererTest {
    private FastsettBeregningsgrunnlagATFLOppdaterer oppdaterer;

    @Mock
    private FastsettBeregningsgrunnlagATFLHåndterer håndterer;

    @Mock
    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste historikk;

    @Mock
    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Mock
    private Behandling behandling;

    @Mock
    private Fagsak fagsak;

    @Mock
    private Aksjonspunkt ap;

    @Before
    public void setup() {
        initMocks(this);
        when(behandling.getFagsak()).thenReturn(fagsak);
        oppdaterer = new FastsettBeregningsgrunnlagATFLOppdaterer(beregningsgrunnlagTjeneste, historikk, håndterer);
    }

    @Test
    public void skal_håndtere_overflødig_fastsett_tidsbegrenset_arbeidsforhold_aksjonspunkt() {
        //Arrange
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagAggregatForBehandling(anyLong())).thenReturn(BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(LocalDate.now()).build());
//        when(beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(anyLong(), any(BeregningsgrunnlagTilstand.class))).thenReturn(Optional.of(new BeregningsgrunnlagGrunnlagEntitet()));

        when(behandling.getÅpentAksjonspunktMedDefinisjonOptional(any())).thenReturn(Optional.of(ap));
        when(ap.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        when(ap.erOpprettet()).thenReturn(true);

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.emptyList(), null);
        // Act
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, ap, dto));

        //Assert
        assertThat(resultat.getEkstraAksjonspunktResultat()).hasSize(1);
        assertThat(resultat.getEkstraAksjonspunktResultat().get(0).getElement1()).isEqualTo(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        assertThat(resultat.getEkstraAksjonspunktResultat().get(0).getElement2()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }
}
