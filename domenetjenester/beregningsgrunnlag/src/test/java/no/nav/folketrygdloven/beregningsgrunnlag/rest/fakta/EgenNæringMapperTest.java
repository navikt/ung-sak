package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.kodeverk.organisasjon.VirksomhetType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.EgenNæringDto;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class EgenNæringMapperTest {

    @Test
    public void skal_mappe_fra_entitet_til_dto() {
        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();

        egenNæringBuilder.medVirksomhetType(VirksomhetType.FISKE);
        egenNæringBuilder.medVirksomhet("923609016");
        egenNæringBuilder.medBegrunnelse("Dette e ren begrunnelse");
        egenNæringBuilder.medBruttoInntekt(BigDecimal.valueOf(123123123));
        egenNæringBuilder.medEndringDato(LocalDate.now().minusMonths(4));
        egenNæringBuilder.medVarigEndring(true);
        egenNæringBuilder.medNyoppstartet(false);

        OppgittEgenNæring egenNæring = egenNæringBuilder.build();

        EgenNæringDto dto = EgenNæringMapper.map(egenNæring);

        assertThat(dto).isNotNull();
        assertThat(dto.getBegrunnelse()).isEqualTo(egenNæring.getBegrunnelse());
        assertThat(dto.getEndringsdato()).isEqualTo(egenNæring.getEndringDato());
        assertThat(dto.getVirksomhetType()).isEqualTo(egenNæring.getVirksomhetType());
        assertThat(dto.getOppgittInntekt()).isEqualTo(egenNæring.getBruttoInntekt());
        assertThat(dto.getOrgnr()).isEqualTo(egenNæring.getOrgnr());
        assertThat(dto.isErVarigEndret()).isEqualTo(egenNæring.getVarigEndring());
        assertThat(dto.isErNyoppstartet()).isEqualTo(egenNæring.getNyoppstartet());
    }

}
