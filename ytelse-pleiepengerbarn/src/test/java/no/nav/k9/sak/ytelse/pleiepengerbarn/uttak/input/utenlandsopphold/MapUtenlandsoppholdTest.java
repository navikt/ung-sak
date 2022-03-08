package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.utenlandsopphold;


import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UtenlandsoppholdPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MapUtenlandsoppholdTest {

    @Test
    void skal_mappe_utenlandsopphold_innenfor_periode_til_vurdering() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Map.of(new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD),
            List.of(new VurdertSøktPeriode<>(periodeTilVurdering, Utfall.OPPFYLT, new Søknadsperiode(periodeTilVurdering))));
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeTilVurdering, Duration.ZERO)),
            List.of(),
            List.of(),
            List.of(new UtenlandsoppholdPeriode(periodeTilVurdering, true, Landkoder.USA, UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)),
            List.of(),
            List.of(),
            List.of()));

        var result = MapUtenlandsopphold.map(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering);

        assertThat(result).hasSize(1);
        var utenlandsopphold = result.get(new LukketPeriode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()));
        assertThat(utenlandsopphold.getUtenlandsoppholdÅrsak()).isEqualTo(no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD);
        assertThat(utenlandsopphold.getLandkode()).isEqualTo("USA");
    }

    @Test
    void skal_mappe_utenlandsopphold_innenfor_periode_til_vurdering_prioriter_siste_verdi() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var journalpostId1 = new JournalpostId(2L);
        var periodeDel1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));
        var periodeDel2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var kravDokumenter = Map.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD),
            List.of(new VurdertSøktPeriode<>(periodeDel1, Utfall.OPPFYLT, new Søknadsperiode(periodeDel1))),
            new KravDokument(journalpostId1, LocalDateTime.now(), KravDokumentType.SØKNAD),
            List.of(new VurdertSøktPeriode<>(periodeDel2, Utfall.OPPFYLT, new Søknadsperiode(periodeDel2))));
        var utenlandsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeDel1.getFomDato(), periodeDel1.getFomDato().plusDays(3));
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel1, Duration.ZERO)),
                List.of(),
                List.of(),
                List.of(new UtenlandsoppholdPeriode(utenlandsperiode, true, Landkoder.USA, UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING )),
                List.of(),
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var result = MapUtenlandsopphold.map(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering);

        assertThat(result).hasSize(1);
        var utenlandsopphold = result.get(new LukketPeriode(utenlandsperiode.getFomDato(), utenlandsperiode.getTomDato()));
        assertThat(utenlandsopphold.getUtenlandsoppholdÅrsak()).isEqualTo(no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING);
        assertThat(utenlandsopphold.getLandkode()).isEqualTo("USA");
    }


}
