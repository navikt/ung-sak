package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.DiagnoseKilde;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.InnleggelsesPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskvilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PeriodeMedKontinuerligTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PeriodeMedUtvidetBehov;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.BostedsAdresse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.Relasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.RelasjonsRolle;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDiagnosekode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

@ApplicationScoped
public class InngangsvilkårOversetter {

    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;

    InngangsvilkårOversetter() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOversetter(OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository,
                                    BehandlingRepository behandlingRepository,
                                    BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.omsorgenForGrunnlagRepository = omsorgenForGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public MedisinskvilkårGrunnlag oversettTilRegelModellMedisinsk(Long behandlingId, DatoIntervallEntitet periode, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        final Periode vilkårsperiode = new Periode(periode.getFomDato(), periode.getTomDato());


        var grunnlag = sykdomGrunnlagBehandling.getGrunnlag();
        final var vilkårsGrunnlag = new MedisinskvilkårGrunnlag(periode.getFomDato(), periode.getTomDato());

        String diagnosekode = null;
        if (grunnlag.getDiagnosekoder() != null) {
            diagnosekode = grunnlag.getDiagnosekoder()
                .getDiagnosekoder()
                .stream()
                .findAny()
                .map(SykdomDiagnosekode::getDiagnosekode)
                .orElse(null);
        }

        List<InnleggelsesPeriode> relevanteInnleggelsesperioder = List.of();
        if (grunnlag.getInnleggelser() != null) {
            relevanteInnleggelsesperioder = grunnlag.getInnleggelser()
                .getPerioder()
                .stream()
                .map(sip -> new Periode(sip.getFom(), sip.getTom()))
                .filter(p -> p.overlaps(vilkårsperiode))
                .map(p -> new InnleggelsesPeriode(p.getFom(), p.getTom()))
                .collect(Collectors.toList());
        }

        final var relevantKontinuerligTilsyn = toTidslinjeFor(grunnlag, SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE)
                .stream()
                .filter(v -> v.getValue().getResultat() == Resultat.OPPFYLT)
                .map(v -> new PeriodeMedKontinuerligTilsyn(v.getFom(), v.getTom()))
                .filter(it -> new Periode(it.getFraOgMed(), it.getTilOgMed()).overlaps(vilkårsperiode))
                .collect(Collectors.toList());

        final var relevantUtvidetBehov = toTidslinjeFor(grunnlag, SykdomVurderingType.TO_OMSORGSPERSONER)
                .stream()
                .filter(v -> v.getValue().getResultat() == Resultat.OPPFYLT)
                .map(v -> new PeriodeMedUtvidetBehov(v.getFom(), v.getTom()))
                .filter(it -> new Periode(it.getFraOgMed(), it.getTilOgMed()).overlaps(vilkårsperiode))
                .collect(Collectors.toList());

        vilkårsGrunnlag.medDiagnoseKilde(DiagnoseKilde.SYKHUSLEGE) // TODO 18-feb
            .medDiagnoseKode(diagnosekode)
            .medInnleggelsesPerioder(relevanteInnleggelsesperioder)
            .medKontinuerligTilsyn(relevantKontinuerligTilsyn)
            .medUtvidetBehov(relevantUtvidetBehov);

        return vilkårsGrunnlag;
    }

    private LocalDateTimeline<SykdomVurderingVersjon> toTidslinjeFor(SykdomGrunnlag grunnlag, SykdomVurderingType type) {
        return SykdomUtils.tilTidslinjeForType(grunnlag.getVurderinger(), type);
    }

    public OmsorgenForVilkårGrunnlag oversettTilRegelModellOmsorgen(Long behandlingId, AktørId aktørId, DatoIntervallEntitet periodeTilVurdering) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periodeTilVurdering).orElseThrow();
        final var omsorgenForGrunnlag = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
        final var pleietrengende = behandlingRepository.hentBehandling(behandlingId).getFagsak().getPleietrengendeAktørId();
        
        // Lar denne stå her inntil videre selv om vi ikke bruker den:
        final var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(aktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        final var pleietrengendeBostedsadresser = personopplysningerAggregat.getAdresserFor(pleietrengende)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        
        // TODO OMSORG: Map inn verdi fremfor null:
        /*
        return new OmsorgenForVilkårGrunnlag(mapReleasjonMellomPleietrengendeOgSøker(personopplysningerAggregat, pleietrengende),
            mapAdresser(søkerBostedsadresser), mapAdresser(pleietrengendeBostedsadresser), omsorgenForGrunnlag.map(OmsorgenForGrunnlag::getOmsorgenFor).map(OmsorgenFor::getHarOmsorgFor).orElse(null));
            */
        
        return new OmsorgenForVilkårGrunnlag(mapReleasjonMellomPleietrengendeOgSøker(personopplysningerAggregat, pleietrengende),
                mapAdresser(søkerBostedsadresser), mapAdresser(pleietrengendeBostedsadresser), null);
    } 

    private List<BostedsAdresse> mapAdresser(List<PersonAdresseEntitet> pleietrengendeBostedsadresser) {
        return pleietrengendeBostedsadresser.stream()
            .map(it -> new BostedsAdresse(it.getAktørId().getId(), it.getAdresselinje1(), it.getAdresselinje2(), it.getAdresselinje3(), it.getPostnummer(), it.getLand()))
            .collect(Collectors.toList());
    }

    private Relasjon mapReleasjonMellomPleietrengendeOgSøker(PersonopplysningerAggregat aggregat, AktørId pleietrengende) {
        final var relasjoner = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengende)).collect(Collectors.toSet());
        if (relasjoner.size() > 1) {
            throw new IllegalStateException("Fant flere relasjoner til barnet. Vet ikke hvilken som skal prioriteres");
        } else if (relasjoner.size() == 1) {
            final var relasjonen = relasjoner.iterator().next();
            return new Relasjon(relasjonen.getAktørId().getId(), relasjonen.getTilAktørId().getId(), RelasjonsRolle.find(relasjonen.getRelasjonsrolle().getKode()), relasjonen.getHarSammeBosted());
        } else {
            return null;
        }
    }
}
