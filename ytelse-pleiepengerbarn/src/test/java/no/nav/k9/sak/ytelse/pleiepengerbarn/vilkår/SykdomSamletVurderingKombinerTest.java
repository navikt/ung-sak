package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;

class SykdomSamletVurderingKombinerTest {

    @Test
    public void happyCase1() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.ktp = dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE);
        tuppel1.innleggelser = dummyInnleggelser();

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.toOp = dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER);


        SykdomSamletVurdering kombinert = tuppel1.kombiner(tuppel2);
        assertThat(kombinert.ktp).isEqualTo(tuppel1.ktp);
        assertThat(kombinert.toOp).isEqualTo(tuppel2.toOp);
        assertThat(kombinert.innleggelser).isEqualTo(tuppel1.innleggelser);
    }

    @Test
    public void happyCase2() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.ktp = dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE);

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.toOp = dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER);

        SykdomSamletVurdering kombinert = tuppel1.kombiner(tuppel2);
        assertThat(kombinert.ktp).isEqualTo(tuppel1.ktp);
        assertThat(kombinert.toOp).isEqualTo(tuppel2.toOp);
        assertThat(kombinert.innleggelser).isNull();
    }

    @Test
    public void happyCase3() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.toOp = dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER);

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.ktp = dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE);
        tuppel2.innleggelser = dummyInnleggelser();

        SykdomSamletVurdering kombinert = tuppel1.kombiner(tuppel2);
        assertThat(kombinert.ktp).isEqualTo(tuppel2.ktp);
        assertThat(kombinert.toOp).isEqualTo(tuppel1.toOp);
        assertThat(kombinert.innleggelser).isEqualTo(tuppel2.innleggelser);
    }

    @Test
    public void sparseKombo() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.innleggelser = dummyInnleggelser();

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.ktp = dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE);


        SykdomSamletVurdering kombinert = tuppel1.kombiner(tuppel2);
        assertThat(kombinert.ktp).isEqualTo(tuppel2.ktp);
        assertThat(kombinert.toOp).isNull();
        assertThat(kombinert.innleggelser).isEqualTo(tuppel1.innleggelser);
    }

    @Test
    public void tillater_ikke_overlappende_tupler_med_forskjellige_vurderinger1() {
         SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
         tuppel1.ktp = dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE);

         SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
         tuppel2.ktp = dummyVurdering(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE);

        assertThrows(IllegalStateException.class, () -> {
            tuppel1.kombiner(tuppel2);
        });
    }

    @Test
    public void tillater_ikke_overlappende_tupler_med_forskjellige_vurderinger2() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.toOp = dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER);

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.toOp = dummyVurdering(SykdomVurderingType.TO_OMSORGSPERSONER);

        assertThrows(IllegalStateException.class, () -> {
            tuppel1.kombiner(tuppel2);
        });
    }

    @Test
    public void tillater_ikke_overlappende_tupler_med_forskjellige_innleggelser() {
        SykdomSamletVurdering tuppel1 = new SykdomSamletVurdering();
        tuppel1.innleggelser = dummyInnleggelser();

        SykdomSamletVurdering tuppel2 = new SykdomSamletVurdering();
        tuppel2.innleggelser = dummyInnleggelser();

        assertThrows(IllegalStateException.class, () -> {
            tuppel1.kombiner(tuppel2);
        });
    }



    public SykdomVurdering dummyVurdering(SykdomVurderingType type) {
        return new SykdomVurdering(type, Collections.emptyList(), "test", LocalDateTime.now());
    }

    public SykdomInnleggelser dummyInnleggelser() {
        return new SykdomInnleggelser(0L, Collections.emptyList(), "test", LocalDateTime.now());
    }
}
