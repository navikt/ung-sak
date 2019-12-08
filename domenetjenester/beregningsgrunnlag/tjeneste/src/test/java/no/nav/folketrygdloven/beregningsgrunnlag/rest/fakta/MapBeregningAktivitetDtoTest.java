package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningAktivitetDto;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

public class MapBeregningAktivitetDtoTest {
    private static final AktørId AKTØRID_1 = AktørId.dummy();
    private static final AktørId AKTØRID_2 = AktørId.dummy();
    private static final AktørId AKTØRID_3 = AktørId.dummy();
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);

    @Test
    public void nyAktivitetIDetteGrunnlaget() {
        // Arrange
        BeregningAktivitetEntitet beregningAktivitet = lagAktivitet(AKTØRID_1);
        List<BeregningAktivitetEntitet> saksbehandledeAktiviteter = List.of();
        List<BeregningAktivitetEntitet> forrigeAktiviteter = List.of(lagAktivitet(AKTØRID_3));
        List<BeregningAktivitetEntitet> forrigeSaksbehandledeAktiviteter = List.of();

        // Act
        BeregningAktivitetDto dto = MapBeregningAktivitetDto.mapBeregningAktivitet(beregningAktivitet, saksbehandledeAktiviteter,
            forrigeAktiviteter, forrigeSaksbehandledeAktiviteter, Optional.empty(), arbeidsgiverTjeneste);

        // Assert
        assertThat(dto.getSkalBrukes()).isNull();
    }

    @Test
    public void aldriSaksbehandletEllerIngenAktiviteterIForrigeSaksbehandlet() {
        // Arrange
        BeregningAktivitetEntitet beregningAktivitet = lagAktivitet(AKTØRID_1);
        List<BeregningAktivitetEntitet> saksbehandledeAktiviteter = List.of();
        List<BeregningAktivitetEntitet> forrigeAktiviteter = List.of(lagAktivitet(AKTØRID_1));
        List<BeregningAktivitetEntitet> forrigeSaksbehandledeAktiviteter = List.of();

        // Act
        BeregningAktivitetDto dto = MapBeregningAktivitetDto.mapBeregningAktivitet(beregningAktivitet, saksbehandledeAktiviteter,
            forrigeAktiviteter, forrigeSaksbehandledeAktiviteter, Optional.empty(), arbeidsgiverTjeneste);

        // Assert
        assertThat(dto.getSkalBrukes()).isNull();
    }


    @Test
    public void tidligereSattTilBenytt() {
        // Arrange
        BeregningAktivitetEntitet beregningAktivitet = lagAktivitet(AKTØRID_1);
        List<BeregningAktivitetEntitet> saksbehandledeAktiviteter = List.of();
        List<BeregningAktivitetEntitet> forrigeAktiviteter = List.of(lagAktivitet(AKTØRID_1));
        List<BeregningAktivitetEntitet> forrigeSaksbehandledeAktiviteter = List.of(lagAktivitet(AKTØRID_1));

        // Act
        BeregningAktivitetDto dto = MapBeregningAktivitetDto.mapBeregningAktivitet(beregningAktivitet, saksbehandledeAktiviteter,
            forrigeAktiviteter, forrigeSaksbehandledeAktiviteter, Optional.empty(), arbeidsgiverTjeneste);

        // Assert
        assertThat(dto.getSkalBrukes()).isTrue();
    }

    @Test
    public void tidligereSattTilIkkeBenytt() {
        // Arrange
        BeregningAktivitetEntitet beregningAktivitet = lagAktivitet(AKTØRID_1);
        List<BeregningAktivitetEntitet> saksbehandledeAktiviteter = List.of();
        List<BeregningAktivitetEntitet> forrigeAktiviteter = List.of(lagAktivitet(AKTØRID_1), lagAktivitet(AKTØRID_2));
        List<BeregningAktivitetEntitet> forrigeSaksbehandledeAktiviteter = List.of(lagAktivitet(AKTØRID_2));

        // Act
        BeregningAktivitetDto dto = MapBeregningAktivitetDto.mapBeregningAktivitet(beregningAktivitet, saksbehandledeAktiviteter,
            forrigeAktiviteter, forrigeSaksbehandledeAktiviteter, Optional.empty(), arbeidsgiverTjeneste);

        // Assert
        assertThat(dto.getSkalBrukes()).isFalse();
    }

    @Test
    public void saksbehandletIDetteGrunnlagetSattTilBenytt() {
        // Arrange
        BeregningAktivitetEntitet beregningAktivitet = lagAktivitet(AKTØRID_1);
        List<BeregningAktivitetEntitet> saksbehandledeAktiviteter = List.of(lagAktivitet(AKTØRID_1));
        List<BeregningAktivitetEntitet> forrigeAktiviteter = List.of(lagAktivitet(AKTØRID_1), lagAktivitet(AKTØRID_2));
        List<BeregningAktivitetEntitet> forrigeSaksbehandledeAktiviteter = List.of();

        // Act
        BeregningAktivitetDto dto = MapBeregningAktivitetDto.mapBeregningAktivitet(beregningAktivitet, saksbehandledeAktiviteter,
            forrigeAktiviteter, forrigeSaksbehandledeAktiviteter, Optional.empty(), arbeidsgiverTjeneste);

        // Assert
        assertThat(dto.getSkalBrukes()).isTrue();
    }

    @Test
    public void saksbehandletIDetteGrunnlagetSattTilIkkeBenytt() {
        // Arrange
        BeregningAktivitetEntitet beregningAktivitet = lagAktivitet(AKTØRID_1);
        List<BeregningAktivitetEntitet> saksbehandledeAktiviteter = List.of(lagAktivitet(AKTØRID_2));
        List<BeregningAktivitetEntitet> forrigeAktiviteter = List.of(lagAktivitet(AKTØRID_1), lagAktivitet(AKTØRID_2));
        List<BeregningAktivitetEntitet> forrigeSaksbehandledeAktiviteter = List.of();

        // Act
        BeregningAktivitetDto dto = MapBeregningAktivitetDto.mapBeregningAktivitet(beregningAktivitet, saksbehandledeAktiviteter,
            forrigeAktiviteter, forrigeSaksbehandledeAktiviteter, Optional.empty(), arbeidsgiverTjeneste);

        // Assert
        assertThat(dto.getSkalBrukes()).isFalse();
    }


    private BeregningAktivitetEntitet lagAktivitet(AktørId aktørId) {
        return BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), TIDENES_ENDE))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.fra(aktørId))
            .build();
    }

}
