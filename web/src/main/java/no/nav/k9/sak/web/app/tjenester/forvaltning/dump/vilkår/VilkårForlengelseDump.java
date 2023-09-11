package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.vilkår;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class VilkårForlengelseDump implements DebugDumpBehandling {


    private final ObjectWriter objectWriter = JsonMapper.getMapper().writerWithDefaultPrettyPrinter();
    private Instance<ForlengelseTjeneste> tjeneste;
    private VilkårTjeneste vilkårTjeneste;

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    VilkårForlengelseDump() {
        // for proxy
    }

    @Inject
    public VilkårForlengelseDump(@Any Instance<ForlengelseTjeneste> tjeneste, VilkårTjeneste vilkårTjeneste,
                                 @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        this.tjeneste = tjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        var vilkårene = vilkårTjeneste.hentVilkårResultat(behandling.getId());
        try {
            var data = ContainerContextRunner.doRun(behandling, () -> utledForlengelseData(vilkårene, ref));
            var content = objectWriter.writeValueAsString(data);
            return List.of(new DumpOutput("vilkår-perioder.json", content));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput("vilkår-perioder-ERROR.txt", sw.toString()));
        }
    }

    private List<VilkårForlengelseDto> utledForlengelseData(Vilkårene vilkårene, BehandlingReferanse ref) {

        return vilkårene.getVilkårene().stream().flatMap(v -> {

            var perioder = v.getPerioder().stream().map(VilkårPeriode::getPeriode).collect(Collectors.toCollection(TreeSet::new));

            var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType());
            var perioderTilVurdering = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), v.getVilkårType());

            try {
                var forlengelseperioder = ForlengelseTjeneste.finnTjeneste(tjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType()).utledPerioderSomSkalBehandlesSomForlengelse(ref,
                    perioder,
                    v.getVilkårType());

                return perioder.stream().map(p -> new VilkårForlengelseDto(p, v.getVilkårType(), perioderTilVurdering.contains(p), forlengelseperioder.contains(p)));
            } catch (IllegalArgumentException e) {
                // Liten hack for å håndtere vilkår som ikkje støtter forlengelse
                return perioder.stream().map(p -> new VilkårForlengelseDto(p, v.getVilkårType(), perioderTilVurdering.contains(p), false));
            }

        }).toList();
    }


}
