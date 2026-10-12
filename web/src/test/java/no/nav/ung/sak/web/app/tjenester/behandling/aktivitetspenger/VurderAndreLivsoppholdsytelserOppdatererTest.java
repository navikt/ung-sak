package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VilkårLivsoppholdsytelserPeriodeVurderingDto;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class VurderAndreLivsoppholdsytelserOppdatererTest {

    private static final Periode PERIODE = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    @Test
    void skal_bruke_ytelseNavn_når_det_er_satt() {
        var dto = new VilkårLivsoppholdsytelserPeriodeVurderingDto(PERIODE, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE, "begrunnelse", "fritekst", "sykepenger");

        assertThat(VurderAndreLivsoppholdsytelserOppdaterer.utledYtelseNavn(dto)).isEqualTo("sykepenger");
    }

    @Test
    void skal_falle_tilbake_til_fritekstVurderingBrev_for_annen_ytelse_når_ytelseNavn_mangler() {
        var dto = new VilkårLivsoppholdsytelserPeriodeVurderingDto(PERIODE, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE, "begrunnelse", "sykepenger");

        assertThat(VurderAndreLivsoppholdsytelserOppdaterer.utledYtelseNavn(dto)).isEqualTo("sykepenger");
    }

    @Test
    void skal_ikke_falle_tilbake_til_fritekstVurderingBrev_for_andre_årsaker() {
        var dto = new VilkårLivsoppholdsytelserPeriodeVurderingDto(PERIODE, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, "begrunnelse", "fritekst");

        assertThat(VurderAndreLivsoppholdsytelserOppdaterer.utledYtelseNavn(dto)).isNull();
    }
}
