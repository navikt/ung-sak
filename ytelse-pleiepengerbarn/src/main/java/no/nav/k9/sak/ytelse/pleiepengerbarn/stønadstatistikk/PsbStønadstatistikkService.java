package no.nav.k9.sak.ytelse.pleiepengerbarn.stønadstatistikk;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.oppdrag.kontrakt.sporing.HenvisningUtleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkArbeidsforhold;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkDiagnosekode;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkInngangsvilkår;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkPeriode;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkUtbetalingsgrad;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkUtfall;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDiagnosekode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.stønadstatistikk.StønadstatistikkPeriodetidslinjebygger.InformasjonTilStønadstatistikkHendelse;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PsbStønadstatistikkService implements StønadstatistikkService {

    private StønadstatistikkPeriodetidslinjebygger stønadstatistikkPeriodetidslinjebygger;
    private BehandlingRepository behandlingRepository;
    private AktørTjeneste aktørTjeneste;
    private SykdomGrunnlagService sykdomGrunnlagService;


    @Inject
    public PsbStønadstatistikkService(StønadstatistikkPeriodetidslinjebygger stønadstatistikkPeriodetidslinjebygger,
            BehandlingRepository behandlingRepository,
            AktørTjeneste aktørTjeneste,
            SykdomGrunnlagService sykdomGrunnlagService) {
        this.stønadstatistikkPeriodetidslinjebygger = stønadstatistikkPeriodetidslinjebygger;
        this.behandlingRepository = behandlingRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
    }

    @Override
    public StønadstatistikkHendelse lagHendelse(UUID behandlingUuid) {
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);
        final PersonIdent søker = aktørTjeneste.hentPersonIdentForAktørId(behandling.getFagsak().getAktørId()).get();
        final PersonIdent pleietrengende= aktørTjeneste.hentPersonIdentForAktørId(behandling.getFagsak().getPleietrengendeAktørId()).get();
        final List<SykdomDiagnosekode> diagnosekoder = sykdomGrunnlagService.hentGrunnlag(behandlingUuid).getGrunnlag().getDiagnosekoder().getDiagnosekoder();
        final LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> periodetidslinje = stønadstatistikkPeriodetidslinjebygger.lagTidslinjeFor(behandling);
        
        final StønadstatistikkHendelse stønadstatistikkHendelse = new StønadstatistikkHendelse(
                behandling.getFagsakYtelseType(),
                søker,
                pleietrengende,
                diagnosekoder.stream().map(d -> new StønadstatistikkDiagnosekode(d.getDiagnosekode())).toList(),
                behandling.getFagsak().getSaksnummer(),
                HenvisningUtleder.utledHenvisning(behandling.getUuid()), // behandlingsreferanse i økonomisystemet: "henvisning"
                behandlingUuid,
                mapPerioder(periodetidslinje)
                );
        
        return stønadstatistikkHendelse;
    }

    private List<StønadstatistikkPeriode> mapPerioder(LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> periodetidslinje) {
        return periodetidslinje.toSegments().stream().map(entry -> mapPeriode(entry)).toList();
    }
    
    private StønadstatistikkPeriode mapPeriode(LocalDateSegment<InformasjonTilStønadstatistikkHendelse> ds) {
        final UttaksperiodeInfo info = ds.getValue().getUttaksperiodeInfo();
        final long bruttoBeregningsgrunnlag = (ds.getValue().getBeregningsgrunnlagDto() != null) ? ds.getValue().getBeregningsgrunnlagDto().getÅrsinntektVisningstall().longValue() : -1; // TODO: OK med presisjonstap?
        return new StønadstatistikkPeriode(
                ds.getFom(),
                ds.getTom(),
                mapUtfall(info.getUtfall()),
                info.getUttaksgrad().intValue(), // OK med presisjonstap?
                mapUtbetalingsgrader(info.getUtbetalingsgrader(), ds.getValue().getBeregningsresultatAndeler()),
                info.getSøkersTapteArbeidstid().intValue(), // OK med presisjonstap?
                info.getOppgittTilsyn(),
                mapÅrsaker(info.getårsaker()),
                mapInngangsvilkår(info.getInngangsvilkår()),
                info.getPleiebehov().intValue(), // OK med presisjonstap?
                bruttoBeregningsgrunnlag
                );
    }
    
    private StønadstatistikkUtfall mapUtfall(Utfall utfall) {
        switch (utfall) {
        case OPPFYLT: return StønadstatistikkUtfall.OPPFYLT;
        case IKKE_OPPFYLT: return StønadstatistikkUtfall.IKKE_OPPFYLT;
        default: throw new IllegalArgumentException("Utfallet '" + utfall.toString() + "' er ikke støttet.");
        }
    }


    private List<StønadstatistikkUtbetalingsgrad> mapUtbetalingsgrader(List<Utbetalingsgrader> utbetalingsgrader, List<BeregningsresultatAndel> beregningsresultatAndeler) {
        return utbetalingsgrader.stream().map(u -> {
            var a = u.getArbeidsforhold();
            final StønadstatistikkArbeidsforhold arbeidsforhold = new StønadstatistikkArbeidsforhold(a.getType(), a.getOrganisasjonsnummer(), a.getAktørId(), a.getArbeidsforholdId());
            
            int utbetalingsgrad = u.getUtbetalingsgrad().intValue(); // TODO: OK med presisjonstap?
            
            final int dagsats;
            if (utbetalingsgrad > 0) {
                final BeregningsresultatAndel andel = finnAndel(arbeidsforhold, beregningsresultatAndeler);
                dagsats = andel.getDagsats(); // TODO: Sjekk om dette er riktig.
            } else {
                dagsats = 0;
            }
            
            return new StønadstatistikkUtbetalingsgrad(
                    arbeidsforhold,
                    u.getNormalArbeidstid(),
                    u.getFaktiskArbeidstid(),
                    utbetalingsgrad,
                    dagsats
                    );
        }).toList();
    }

    private BeregningsresultatAndel finnAndel(StønadstatistikkArbeidsforhold arbeidsforhold, List<BeregningsresultatAndel> beregningsresultatAndeler) {
        final List<BeregningsresultatAndel> kandidater = beregningsresultatAndeler.stream()
                .filter(a -> arbeidsforhold.getOrganisasjonsnummer().equals(a.getArbeidsforholdIdentifikator()) || arbeidsforhold.getAktørId().equals(a.getArbeidsforholdIdentifikator()))
                .toList();
        
        final List<BeregningsresultatAndel> andelsliste = kandidater.stream().filter(a -> arbeidsforhold.getArbeidsforholdId() != null && arbeidsforhold.getArbeidsforholdId().equals(a.getArbeidsforholdRef().getReferanse())).toList();
        if (andelsliste.size() == 1) {
            return andelsliste.get(0);
        } else if (andelsliste.size() > 1) {
            throw new IllegalStateException("Fant mer enn én andel med arbeidsforholdref.");
        }
        
        if (kandidater.size() > 1) {
            throw new IllegalStateException("Fant mer enn én andel uten arbeidsforholdref.");
        }
        
        if (kandidater.isEmpty()) {
            throw new IllegalStateException("Fant ingen andel i andeler: " + Arrays.toString(beregningsresultatAndeler.toArray()) + ", arbeidsforhold: " + arbeidsforhold.toString());
        }
        
        return kandidater.get(0);
    }

    private List<String> mapÅrsaker(Set<Årsak> årsaker) {
        return årsaker.stream().map(å -> å.toString()).toList();
    }
    
    private List<StønadstatistikkInngangsvilkår> mapInngangsvilkår(Map<String, Utfall> inngangsvilkår) {
        return inngangsvilkår.entrySet().stream().map(entry -> new StønadstatistikkInngangsvilkår(entry.getKey(), mapUtfall(entry.getValue()))).toList();
    }
}
