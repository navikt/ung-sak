package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.DiagnoseKilde;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.InnleggelsesPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskvilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PeriodeMedKontinuerligTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PeriodeMedUtvidetBehov;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.BostedsAdresse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.OmsorgenForGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.Relasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.RelasjonsRolle;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.OmsorgenFor;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType;

@ApplicationScoped
public class InngangsvilkårOversetter {

    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;

    InngangsvilkårOversetter() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOversetter(MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                    BehandlingRepository behandlingRepository,
                                    BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public MedisinskvilkårGrunnlag oversettTilRegelModellMedisinsk(Long behandlingId, DatoIntervallEntitet periode, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        final Periode vilkårsperiode = new Periode(periode.getFomDato(), periode.getTomDato());

        final var vilkårsGrunnlag = new MedisinskvilkårGrunnlag(periode.getFomDato(), periode.getTomDato());
        
        var grunnlag = sykdomGrunnlagBehandling.getGrunnlag();
        
        String diagnosekode = null;
        if (grunnlag.getDiagnosekoder() != null) {
            diagnosekode = grunnlag.getDiagnosekoder()
                .getDiagnosekoder()
                .stream()
                .findAny()
                .map(d -> d.getDiagnosekode())
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

        final var relevantKontinuerligTilsyn = SykdomUtils.tilTidslinje(grunnlag.getVurderinger())
                .stream()
                .filter(v -> v.getValue().getSykdomVurdering().getType() == SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE)
                .filter(v -> v.getValue().getResultat() == Resultat.OPPFYLT)
                .map(v -> new PeriodeMedKontinuerligTilsyn(v.getFom(), v.getTom()))
                .filter(it -> new Periode(it.getFraOgMed(), it.getTilOgMed()).overlaps(vilkårsperiode))
                .collect(Collectors.toList());
        
        final var relevantUtvidetBehov = SykdomUtils.tilTidslinje(grunnlag.getVurderinger())
                .stream()
                .filter(v -> v.getValue().getSykdomVurdering().getType() == SykdomVurderingType.TO_OMSORGSPERSONER)
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

    public OmsorgenForGrunnlag oversettTilRegelModellOmsorgen(Long behandlingId, AktørId aktørId, DatoIntervallEntitet periodeTilVurdering) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periodeTilVurdering).orElseThrow();
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId);
        final var pleietrengende = behandlingRepository.hentBehandling(behandlingId).getFagsak().getPleietrengendeAktørId();
        final var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(aktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        final var pleietrengendeBostedsadresser = personopplysningerAggregat.getAdresserFor(pleietrengende)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        return new OmsorgenForGrunnlag(mapReleasjonMellomPleietrengendeOgSøker(personopplysningerAggregat, pleietrengende),
            mapAdresser(søkerBostedsadresser), mapAdresser(pleietrengendeBostedsadresser), medisinskGrunnlag.map(MedisinskGrunnlag::getOmsorgenFor).map(OmsorgenFor::getHarOmsorgFor).orElse(null));
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
