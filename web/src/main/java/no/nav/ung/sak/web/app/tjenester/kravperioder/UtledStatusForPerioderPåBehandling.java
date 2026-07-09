package no.nav.ung.sak.web.app.tjenester.kravperioder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramOpphørUtleder;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.kontrakt.krav.KravDokumentMedSøktePerioder;
import no.nav.ung.sak.kontrakt.krav.KravDokumentType;
import no.nav.ung.sak.kontrakt.krav.PeriodeMedÅrsaker;
import no.nav.ung.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakMedPerioder;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.ung.sak.søknadsfrist.KravDokument;
import no.nav.ung.sak.søknadsfrist.SøktPeriode;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;

class UtledStatusForPerioderPåBehandling {

    public static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT,
        BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS,
        BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
        BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT,
        BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO,
        BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER,
        BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);

    static StatusForPerioderPåBehandling utledStatus(Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterTilBehandling,
                                                     List<Trigger> prosesstriggere,
                                                     Behandling behandling,
                                                     UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        var årsakstidslinje = finnÅrsakstidslinje(kravdokumenterTilBehandling, prosesstriggere, behandling, ungdomsprogramPeriodeRepository);
        var periodeMedÅrsaker = mapPeriodeMedÅrsaker(årsakstidslinje);
        var årsakerMedPerioder = finnÅrsakerMedPerioder(periodeMedÅrsaker);
        return new StatusForPerioderPåBehandling(
            periodeMedÅrsaker.stream().map(PeriodeMedÅrsaker::getPeriode).collect(Collectors.toSet()),
            periodeMedÅrsaker,
            årsakerMedPerioder,
            mapKravTilDto(kravdokumenterTilBehandling)
        );
    }

    private static List<PeriodeMedÅrsaker> mapPeriodeMedÅrsaker(LocalDateTimeline<Set<ÅrsakTilVurdering>> årsakstidslinje) {
        return årsakstidslinje.stream()
            .map(p -> new PeriodeMedÅrsaker(new Periode(p.getFom(), p.getTom()), p.getValue()))
            .toList();
    }

    private static LocalDateTimeline<Set<ÅrsakTilVurdering>> finnÅrsakstidslinje(Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterTilBehandling,
                                                                                List<Trigger> prosesstriggere,
                                                                                Behandling behandling,
                                                                                UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        var årsakerFraTriggere = finnÅrsakerFraTriggereTidslinje(prosesstriggere, behandling, ungdomsprogramPeriodeRepository);
        var søknadtidslinje = finnFørsteSøknadTidslinje(kravdokumenterTilBehandling);
        return søknadtidslinje.crossJoin(årsakerFraTriggere, StandardCombinators::union);
    }

    private static List<ÅrsakMedPerioder> finnÅrsakerMedPerioder(List<PeriodeMedÅrsaker> periodeMedÅrsaker) {
        var perioderPrÅrsak = new HashMap<ÅrsakTilVurdering, Set<Periode>>();
        for (var årsakerOgPeriode : periodeMedÅrsaker) {
            for (var årsak : årsakerOgPeriode.getÅrsaker()) {
                var eksisterende = perioderPrÅrsak.getOrDefault(årsak, new TreeSet<>());
                eksisterende.add(årsakerOgPeriode.getPeriode());
                perioderPrÅrsak.replace(årsak, eksisterende);
            }
        }

        return perioderPrÅrsak.entrySet().stream().map(p -> new ÅrsakMedPerioder(p.getKey(), p.getValue()))
            .toList();
    }


    private static LocalDateTimeline<Set<ÅrsakTilVurdering>> finnÅrsakerFraTriggereTidslinje(List<Trigger> prosesstriggere,
                                                                                             Behandling behandling,
                                                                                             UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        // RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM regnes som utdatert/stale når
        // RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM også finnes på behandlingen — jf. samme
        // mønster i UngEtterlysningOppretter og BehandlingDtoUtil.
        boolean harOpphevelse = prosesstriggere.stream()
            .anyMatch(it -> it.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM);

        // Skiller mellom reell opphevelse (opphøret ble faktisk vedtatt tidligere) og annullering
        // (opphøret ble aldri iverksatt, jf. UngdomsprogramOpphørUtleder) — samme skille som i BehandlingDtoUtil.
        boolean opphørVarFaktiskIverksatt = harOpphevelse
            && UngdomsprogramOpphørUtleder.opphørAvUngdomsprogrammetVarInkludertIVedtaket(behandling, ungdomsprogramPeriodeRepository);

        return prosesstriggere.stream()
            .filter(it -> RELEVANTE_ÅRSAKER.contains(it.getÅrsak()))
            .filter(it -> !(harOpphevelse && it.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM))
            .map(it -> new LocalDateTimeline<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(),
                Set.of(mapTilÅrsakTilVurdering(it.getÅrsak(), opphørVarFaktiskIverksatt))))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static ÅrsakTilVurdering mapTilÅrsakTilVurdering(BehandlingÅrsakType årsak, boolean opphørVarFaktiskIverksatt) {
        if (årsak == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM && !opphørVarFaktiskIverksatt) {
            return ÅrsakTilVurdering.OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING_UNGDOMSPROGRAM;
        }
        return ÅrsakTilVurdering.mapFra(årsak);
    }

    private static LocalDateTimeline<Set<ÅrsakTilVurdering>> finnFørsteSøknadTidslinje(Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterTilBehandling) {
        return kravdokumenterTilBehandling.values().stream()
            .flatMap(Collection::stream)
            .map(SøktPeriode::getPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }


    private static List<KravDokumentMedSøktePerioder> mapKravTilDto(Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> relevanteDokumenterMedPeriode) {
        return relevanteDokumenterMedPeriode.entrySet()
            .stream()
            .map(it -> new KravDokumentMedSøktePerioder(it.getKey().getJournalpostId(),
                it.getKey().getInnsendingsTidspunkt(),
                KravDokumentType.fraKode(it.getKey().getType().name()),
                it.getValue().stream().map(SøktPeriode::getPeriode).map(p -> new no.nav.ung.sak.kontrakt.krav.SøktPeriode(new Periode(p.getFomDato(), p.getTomDato()))).toList(),
                it.getKey().getKildesystem().getKode()))
            .toList();

    }
}
