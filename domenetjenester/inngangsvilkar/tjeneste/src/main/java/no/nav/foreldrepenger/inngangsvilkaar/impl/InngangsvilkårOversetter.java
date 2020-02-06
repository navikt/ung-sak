package no.nav.foreldrepenger.inngangsvilkaar.impl;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.FinnOmSøkerHarArbeidsforholdOgInntekt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.MedisinskvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.PersonStatusType;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@ApplicationScoped
public class InngangsvilkårOversetter {

    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    InngangsvilkårOversetter() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOversetter(BehandlingRepositoryProvider repositoryProvider,
                                    BasisPersonopplysningTjeneste personopplysningTjeneste,
                                    InntektArbeidYtelseTjeneste iayTjeneste) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.iayTjeneste = iayTjeneste;
        this.medlemskapPerioderTjeneste = new MedlemskapPerioderTjeneste();
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public MedlemskapsvilkårGrunnlag oversettTilRegelModellMedlemskap(BehandlingReferanse ref, DatoIntervallEntitet periode) {
        Long behandlingId = ref.getBehandlingId();
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);

        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);
        MedlemskapsvilkårGrunnlag grunnlag = new MedlemskapsvilkårGrunnlag(
            brukerErMedlemEllerIkkeRelevantPeriode(medlemskap, personopplysninger, ref.getSkjæringstidspunkt()), // FP VK 2.13
            tilPersonStatusType(personopplysninger), // FP VK 2.1
            brukerNorskNordisk(personopplysninger), // FP VK 2.11
            brukerBorgerAvEOS(vurdertMedlemskap, personopplysninger)); // FP VIK 2.12

        Optional<InntektArbeidYtelseGrunnlag> iayOpt = iayTjeneste.finnGrunnlag(behandlingId);
        grunnlag.setHarSøkerArbeidsforholdOgInntekt(FinnOmSøkerHarArbeidsforholdOgInntekt.finn(iayOpt, ref.getUtledetSkjæringstidspunkt(), ref.getAktørId()));

        // defaulter uavklarte fakta til true
        grunnlag.setBrukerAvklartLovligOppholdINorge(
            vurdertMedlemskap.map(VurdertMedlemskap::getLovligOppholdVurdering).orElse(true));
        grunnlag.setBrukerAvklartBosatt(
            vurdertMedlemskap.map(VurdertMedlemskap::getBosattVurdering).orElse(true));
        grunnlag.setBrukerAvklartOppholdsrett(
            vurdertMedlemskap.map(VurdertMedlemskap::getOppholdsrettVurdering).orElse(true));

        // FP VK 2.2 Er bruker avklart som pliktig eller frivillig medlem?
        grunnlag.setBrukerAvklartPliktigEllerFrivillig(erAvklartSomPliktigEllerFrivilligMedlem(medlemskap, ref.getSkjæringstidspunkt()));

        return grunnlag;
    }

    /**
     * True dersom saksbehandler har vurdert til å være medlem i relevant periode
     */
    private boolean erAvklartSomPliktigEllerFrivilligMedlem(Optional<MedlemskapAggregat> medlemskap, Skjæringstidspunkt skjæringstidspunkter) {
        if (medlemskap.isPresent()) {
            Optional<VurdertMedlemskap> vurdertMedlemskapOpt = medlemskap.get().getVurdertMedlemskap();
            if (vurdertMedlemskapOpt.isPresent()) {
                VurdertMedlemskap vurdertMedlemskap = vurdertMedlemskapOpt.get();
                if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                    MedlemskapManuellVurderingType.MEDLEM.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                    return true;
                }
                if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                    MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                    return false;
                }
            }
            return medlemskapPerioderTjeneste.brukerMaskineltAvklartSomFrivilligEllerPliktigMedlem(
                medlemskap.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()),
                skjæringstidspunkter.getUtledetSkjæringstidspunkt());
        } else {
            return false;
        }
    }

    /**
     * True dersom saksbehandler har vurdert til ikke å være medlem i relevant periode
     */
    private boolean erAvklartSomIkkeMedlem(Optional<VurdertMedlemskap> medlemskap) {
        return medlemskap.isPresent() && medlemskap.get().getMedlemsperiodeManuellVurdering() != null
            && MedlemskapManuellVurderingType.UNNTAK.equals(medlemskap.get().getMedlemsperiodeManuellVurdering());
    }

    private boolean brukerErMedlemEllerIkkeRelevantPeriode(Optional<MedlemskapAggregat> medlemskap, PersonopplysningerAggregat søker,
                                                           Skjæringstidspunkt skjæringstidspunkter) {
        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);
        if (vurdertMedlemskap.isPresent()
            && MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.get().getMedlemsperiodeManuellVurdering())) {
            return true;
        }

        Set<MedlemskapPerioderEntitet> medlemskapPerioder = medlemskap.isPresent() ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptySet();
        boolean erAvklartMaskineltSomIkkeMedlem = medlemskapPerioderTjeneste.brukerMaskineltAvklartSomIkkeMedlem(søker,
            medlemskapPerioder, skjæringstidspunkter.getUtledetSkjæringstidspunkt());
        boolean erAvklartManueltSomIkkeMedlem = erAvklartSomIkkeMedlem(vurdertMedlemskap);

        return !(erAvklartMaskineltSomIkkeMedlem || erAvklartManueltSomIkkeMedlem);
    }

    private boolean brukerBorgerAvEOS(Optional<VurdertMedlemskap> medlemskap, PersonopplysningerAggregat aggregat) {
        // Tar det første for det er det som er prioritert høyest rangert på region
        final Optional<StatsborgerskapEntitet> statsborgerskap = aggregat.getStatsborgerskapFor(aggregat.getSøker().getAktørId()).stream().findFirst();
        return medlemskap
            .map(VurdertMedlemskap::getErEøsBorger)
            .orElse(Region.EOS.equals(statsborgerskap.map(StatsborgerskapEntitet::getRegion).orElse(null)));
    }

    private boolean brukerNorskNordisk(PersonopplysningerAggregat aggregat) {
        // Tar det første for det er det som er prioritert høyest rangert på region
        final Optional<StatsborgerskapEntitet> statsborgerskap = aggregat.getStatsborgerskapFor(aggregat.getSøker().getAktørId()).stream().findFirst();
        return Region.NORDEN.equals(statsborgerskap.map(StatsborgerskapEntitet::getRegion).orElse(null));
    }

    private PersonStatusType tilPersonStatusType(PersonopplysningerAggregat personopplysninger) {
        // Bruker overstyrt personstatus hvis det finnes
        PersonstatusType type = Optional.ofNullable(personopplysninger.getPersonstatusFor(personopplysninger.getSøker().getAktørId()))
            .map(PersonstatusEntitet::getPersonstatus).orElse(null);

        if (PersonstatusType.BOSA.equals(type) || PersonstatusType.ADNR.equals(type)) {
            return PersonStatusType.BOSA;
        } else if (PersonstatusType.UTVA.equals(type)) {
            return PersonStatusType.UTVA;
        } else if (PersonstatusType.erDød(type)) {
            return PersonStatusType.DØD;
        }
        return null;
    }

    public VilkårData tilVilkårData(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag, DatoIntervallEntitet periode) {
        return new VilkårUtfallOversetter().oversett(vilkårType, evaluation, grunnlag, periode);
    }

    public MedisinskvilkårGrunnlag oversettTilRegelModellMedisinsk(BehandlingReferanse ref, DatoIntervallEntitet periode) {
        return new MedisinskvilkårGrunnlag(true); // FIXME (k9) : Map reele data inn
    }
}
