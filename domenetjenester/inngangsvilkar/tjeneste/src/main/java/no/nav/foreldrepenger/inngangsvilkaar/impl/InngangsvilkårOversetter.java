package no.nav.foreldrepenger.inngangsvilkaar.impl;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.KontinuerligTilsynPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæring;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Pleietrengende;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.FinnOmSøkerHarArbeidsforholdOgInntekt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.DiagnoseKilde;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.InnleggelsesPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.MedisinskvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.PeriodeMedKontinuerligTilsyn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.PeriodeMedUtvidetBehov;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.PersonStatusType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor.BostedsAdresse;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor.OmsorgenForGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor.Relasjon;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor.RelasjonsRolle;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medisinsk.LegeerklæringKilde;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class InngangsvilkårOversetter {

    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    InngangsvilkårOversetter() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOversetter(MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                    BasisPersonopplysningTjeneste personopplysningTjeneste,
                                    InntektArbeidYtelseTjeneste iayTjeneste,
                                    MedlemskapRepository medlemskapRepository) {
        this.medlemskapRepository = medlemskapRepository;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.iayTjeneste = iayTjeneste;
        this.medlemskapPerioderTjeneste = new MedlemskapPerioderTjeneste();
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public VilkårData tilVilkårData(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag, DatoIntervallEntitet periode) {
        return new VilkårUtfallOversetter().oversett(vilkårType, evaluation, grunnlag, periode);
    }

    public MedisinskvilkårGrunnlag oversettTilRegelModellMedisinsk(Long behandlingId, DatoIntervallEntitet periode) {
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId);

        final var vilkårsGrunnlag = new MedisinskvilkårGrunnlag(periode.getFomDato(), periode.getTomDato());
        if (medisinskGrunnlag.isPresent()) {
            final var grunnlag = medisinskGrunnlag.get();
            final var relevanteLegeerklæringer = grunnlag.getLegeerklæringer()
                .getLegeerklæringer()
                .stream()
                .filter(it -> it.getPeriode().overlapper(periode))
                .collect(Collectors.toList());
            final var relevantKontinuerligTilsyn = grunnlag.getKontinuerligTilsyn()
                .getPerioder()
                .stream()
                .filter(it -> it.getPeriode().overlapper(periode))
                .collect(Collectors.toList());

            vilkårsGrunnlag.medDiagnoseKilde(utledKildeFraLegeerklæringer(relevanteLegeerklæringer))
                .medDiagnoseKode(utledDiagnose(relevanteLegeerklæringer))
                .medInnleggelsesPerioder(mapInnleggelsesPerioder(relevanteLegeerklæringer))
                .medKontinuerligTilsyn(mapKontinuerligTilsyn(relevantKontinuerligTilsyn))
                .medUtvidetBehov(mapUtvidetTilsyn(relevantKontinuerligTilsyn));
        }
        return vilkårsGrunnlag;
    }

    private List<PeriodeMedUtvidetBehov> mapUtvidetTilsyn(List<KontinuerligTilsynPeriode> relevantKontinuerligTilsyn) {
        return relevantKontinuerligTilsyn.stream()
            .filter(it -> it.getGrad() == 200)
            .map(it -> new PeriodeMedUtvidetBehov(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toList());
    }

    private List<PeriodeMedKontinuerligTilsyn> mapKontinuerligTilsyn(List<KontinuerligTilsynPeriode> relevantKontinuerligTilsyn) {
        return relevantKontinuerligTilsyn.stream()
            .filter(it -> it.getGrad() == 100)
            .map(it -> new PeriodeMedKontinuerligTilsyn(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toList());
    }

    private List<InnleggelsesPeriode> mapInnleggelsesPerioder(List<Legeerklæring> relevanteLegeerklæringer) {
        return relevanteLegeerklæringer.stream()
            .map(Legeerklæring::getInnleggelsesPerioder)
            .flatMap(Collection::stream)
            .map(it -> new InnleggelsesPeriode(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toList());
    }

    private String utledDiagnose(List<Legeerklæring> legeerklæringer) {
        return legeerklæringer.stream()
            .filter(it -> it.getDiagnose() != null)
            .filter(it -> !it.getDiagnose().isEmpty())
            .filter(it -> !it.getDiagnose().isBlank())
            .min(Comparator.comparing(Legeerklæring::getPeriode, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(Legeerklæring::getDiagnose)
            .orElse(null);
    }

    private DiagnoseKilde utledKildeFraLegeerklæringer(List<Legeerklæring> legeerklæringer) {
        final var kilder = legeerklæringer
            .stream()
            .map(Legeerklæring::getKilde)
            .collect(Collectors.toSet());
        if (kilder.contains(LegeerklæringKilde.SYKEHUSLEGE)) {
            return DiagnoseKilde.SYKHUSLEGE;
        }
        if (kilder.contains(LegeerklæringKilde.SPESIALISTHELSETJENESTE)) {
            return DiagnoseKilde.SPESIALISTHELSETJENESTEN;
        }
        if (kilder.contains(LegeerklæringKilde.FASTLEGE)) {
            return DiagnoseKilde.FASTLEGE;
        }
        return DiagnoseKilde.ANNET;
    }

    public OmsorgenForGrunnlag oversettTilRegelModellOmsorgen(Long behandlingId, AktørId aktørId, DatoIntervallEntitet periodeTilVurdering) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periodeTilVurdering).orElseThrow();
        final var pleietrengende = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId).map(MedisinskGrunnlag::getPleietrengende).map(Pleietrengende::getAktørId).orElseThrow();
        final var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(aktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        final var pleietrengendeBostedsadresser = personopplysningerAggregat.getAdresserFor(pleietrengende)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        return new OmsorgenForGrunnlag(mapReleasjonMellomPleietrengendeOgSøker(personopplysningerAggregat, pleietrengende),
            mapAdresser(søkerBostedsadresser), mapAdresser(pleietrengendeBostedsadresser));
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
