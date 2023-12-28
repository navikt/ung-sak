package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.KalkulusStartpunktUtleder;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
public class BeregningStartpunktDump implements DebugDumpBehandling {

    private KalkulusStartpunktUtleder kalkulusStartpunktUtleder;

    BeregningStartpunktDump() {
        // for proxys
    }

    @Inject
    BeregningStartpunktDump(KalkulusStartpunktUtleder kalkulusStartpunktUtleder) {
        this.kalkulusStartpunktUtleder = kalkulusStartpunktUtleder;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        List<Tuple<BehandlingStegType, PeriodeTilVurdering>> lista = new ArrayList<>();

        Map<BehandlingStegType, NavigableSet<PeriodeTilVurdering>> startpunkt = kalkulusStartpunktUtleder.utledPerioderPrStartpunkt(BehandlingReferanse.fra(behandling));
        startpunkt.forEach((steg, perioder) -> perioder.forEach(periode -> lista.add(new Tuple<>(steg, periode))));

        Function<Tuple<BehandlingStegType, PeriodeTilVurdering>, String> kolonneSteg = a -> a.getElement1().getKode();
        Function<Tuple<BehandlingStegType, PeriodeTilVurdering>, LocalDate> kolonneFom = a -> a.getElement2().getPeriode().getFomDato();
        Function<Tuple<BehandlingStegType, PeriodeTilVurdering>, LocalDate> kolonneTom = a -> a.getElement2().getPeriode().getTomDato();
        Function<Tuple<BehandlingStegType, PeriodeTilVurdering>, Boolean> kolonneForlengelse = a -> a.getElement2().erForlengelse();
        Function<Tuple<BehandlingStegType, PeriodeTilVurdering>, Boolean> kolonneEndringUttak = a -> a.getElement2().erEndringIUttak();

        var toCsv = new LinkedHashMap<String, Function<Tuple<BehandlingStegType, PeriodeTilVurdering>, ?>>();
        toCsv.put("fom", kolonneFom);
        toCsv.put("tom", kolonneTom);
        toCsv.put("steg", kolonneSteg);
        toCsv.put("erForlengelse", kolonneForlengelse);
        toCsv.put("endringIUttak", kolonneEndringUttak);

        String path = "beregning-startpunkt.csv";
        DumpOutput dumpOutput = CsvOutput.dumpAsCsv(true, lista, basePath + "/" + path, toCsv);
        dumpMottaker.newFile(dumpOutput.getPath());
        dumpMottaker.write(dumpOutput.getContent());
    }
}
