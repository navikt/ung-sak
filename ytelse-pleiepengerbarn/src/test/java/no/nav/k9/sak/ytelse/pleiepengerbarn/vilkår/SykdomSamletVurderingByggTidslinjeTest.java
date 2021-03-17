package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;

class SykdomSamletVurderingByggTidslinjeTest {

    @Test
    public void happyCase1() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.setKtp(dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE));
        tuppel1.setInnleggelser(dummyInnleggelser());

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.setToOp(dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER));


        SykdomSamletVurdering kombinert = tuppel1.kombinerForSammeTidslinje(tuppel2);
        assertThat(kombinert.getKtp()).isEqualTo(tuppel1.getKtp());
        assertThat(kombinert.getToOp()).isEqualTo(tuppel2.getToOp());
        assertThat(kombinert.getInnleggelser()).isEqualTo(tuppel1.getInnleggelser());
    }

    @Test
    public void happyCase2() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.setKtp(dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE));

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.setToOp(dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER));

        SykdomSamletVurdering kombinert = tuppel1.kombinerForSammeTidslinje(tuppel2);
        assertThat(kombinert.getKtp()).isEqualTo(tuppel1.getKtp());
        assertThat(kombinert.getToOp()).isEqualTo(tuppel2.getToOp());
        assertThat(kombinert.getInnleggelser()).isNull();
    }

    @Test
    public void happyCase3() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.setToOp(dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER));

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.setKtp(dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE));
        tuppel2.setInnleggelser(dummyInnleggelser());

        SykdomSamletVurdering kombinert = tuppel1.kombinerForSammeTidslinje(tuppel2);
        assertThat(kombinert.getKtp()).isEqualTo(tuppel2.getKtp());
        assertThat(kombinert.getToOp()).isEqualTo(tuppel1.getToOp());
        assertThat(kombinert.getInnleggelser()).isEqualTo(tuppel2.getInnleggelser());
    }

    @Test
    public void sparseKombo() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.setInnleggelser(dummyInnleggelser());

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.setKtp(dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE));


        SykdomSamletVurdering kombinert = tuppel1.kombinerForSammeTidslinje(tuppel2);
        assertThat(kombinert.getKtp()).isEqualTo(tuppel2.getKtp());
        assertThat(kombinert.getToOp()).isNull();
        assertThat(kombinert.getInnleggelser()).isEqualTo(tuppel1.getInnleggelser());
    }

    @Test
    public void tillater_ikke_overlappende_tupler_med_forskjellige_vurderinger1() {
         SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
         tuppel1.setKtp(dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE));

         SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
         tuppel2.setKtp(dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE));

        assertThrows(IllegalStateException.class, () -> {
            tuppel1.kombinerForSammeTidslinje(tuppel2);
        });
    }

    @Test
    public void tillater_ikke_overlappende_tupler_med_forskjellige_vurderinger2() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.setToOp(dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER));

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.setToOp(dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER));

        assertThrows(IllegalStateException.class, () -> {
            tuppel1.kombinerForSammeTidslinje(tuppel2);
        });
    }

    @Test
    public void tillater_ikke_overlappende_tupler_med_forskjellige_innleggelser() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.setInnleggelser(dummyInnleggelser());

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.setInnleggelser(dummyInnleggelser());

        assertThrows(IllegalStateException.class, () -> {
            tuppel1.kombinerForSammeTidslinje(tuppel2);
        });
    }



    public SykdomVurdering dummyVurdering(SykdomVurderingType type) {
        return new SykdomVurdering(type, Collections.emptyList(), "test", LocalDateTime.now());
    }

    public SykdomInnleggelser dummyInnleggelser() {
        return new SykdomInnleggelser(0L, Collections.emptyList(), "test", LocalDateTime.now());
    }
}
