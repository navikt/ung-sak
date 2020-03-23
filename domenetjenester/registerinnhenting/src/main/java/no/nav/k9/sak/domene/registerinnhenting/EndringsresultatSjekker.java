package no.nav.k9.sak.domene.registerinnhenting;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;

@Dependent
public class EndringsresultatSjekker {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private OpptjeningRepository opptjeningRepository;
    private KalkulusTjeneste kalkulusTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    EndringsresultatSjekker() {
        // For CDI
    }

    @Inject
    public EndringsresultatSjekker(PersonopplysningTjeneste personopplysningTjeneste,
                                   MedlemTjeneste medlemTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   KalkulusTjeneste kalkulusTjeneste,
                                   BehandlingRepositoryProvider provider) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningRepository = provider.getOpptjeningRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
    }

    public EndringsresultatSnapshot opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(Long behandlingId) {
        EndringsresultatSnapshot snapshot = EndringsresultatSnapshot.opprett();
        snapshot.leggTil(personopplysningTjeneste.finnAktivGrunnlagId(behandlingId));
        snapshot.leggTil(medlemTjeneste.finnAktivGrunnlagId(behandlingId));

        EndringsresultatSnapshot iaySnapshot =  inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId)
                .map(iayg -> EndringsresultatSnapshot.medSnapshot(InntektArbeidYtelseGrunnlag.class, iayg.getEksternReferanse()))
                .orElse(EndringsresultatSnapshot.utenSnapshot(InntektArbeidYtelseGrunnlag.class));

        snapshot.leggTil(iaySnapshot);

        return snapshot;
    }

    public EndringsresultatDiff finnSporedeEndringerPåBehandlingsgrunnlag(Long behandlingId, EndringsresultatSnapshot idSnapshotFør) {
        final boolean kunSporedeEndringer = true;
        // Del 1: Finn diff mellom grunnlagets id før og etter oppdatering
        EndringsresultatSnapshot idSnapshotNå = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);
        EndringsresultatDiff idDiff = idSnapshotNå.minus(idSnapshotFør);

        // Del 2: Transformer diff på grunnlagets id til diff på grunnlagets sporede endringer (@ChangeTracked)
        EndringsresultatDiff sporedeEndringerDiff = EndringsresultatDiff.opprettForSporingsendringer();
        idDiff.hentDelresultat(PersonInformasjonEntitet.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> personopplysningTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        idDiff.hentDelresultat(MedlemskapAggregat.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> medlemTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        idDiff.hentDelresultat(InntektArbeidYtelseGrunnlag.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> {
                InntektArbeidYtelseGrunnlag grunnlag1 = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, (UUID)idEndring.getGrunnlagId1());
                InntektArbeidYtelseGrunnlag grunnlag2 = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, (UUID)idEndring.getGrunnlagId2());
                return new IAYGrunnlagDiff(grunnlag1, grunnlag2).diffResultat(kunSporedeEndringer);
            }));
        return sporedeEndringerDiff;
    }

    public EndringsresultatSnapshot opprettEndringsresultatIdPåBehandlingSnapshot(Behandling behandling) {
        Long behandlingId = behandling.getId();
        EndringsresultatSnapshot snapshot = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);

        snapshot.leggTil(opptjeningRepository.finnAktivGrunnlagId(behandling));
        snapshot.leggTil(finnAktivBeregningsgrunnlagGrunnlagAggregatId(behandlingId));

        // Resultatstrukturene nedenfor støtter ikke paradigme med "aktivt" grunnlag som kan identifisere med id
        // Aksepterer her at endringssjekk heller utledes av deres tidsstempel forutsatt at metoden ikke brukes i
        // kritiske endringssjekker. Håp om at de i fremtiden vil støtte paradigme.
        snapshot.leggTil(lagVilkårResultatIdSnapshotAvTidsstempel(behandling));

        return snapshot;
    }

    public EndringsresultatDiff finnIdEndringerPåBehandling(Behandling behandling, EndringsresultatSnapshot idSnapshotFør) {
        EndringsresultatSnapshot idSnapshotNå = opprettEndringsresultatIdPåBehandlingSnapshot(behandling);
        return idSnapshotNå.minus(idSnapshotFør);
    }

    private EndringsresultatSnapshot lagVilkårResultatIdSnapshotAvTidsstempel(Behandling behandling) {
       return vilkårResultatRepository.hentHvisEksisterer(behandling.getId())
                 .map(vilkårResultat ->
                     EndringsresultatSnapshot.medSnapshot(Vilkårene.class, hentLongVerdiAvEndretTid(vilkårResultat)))
                 .orElse(EndringsresultatSnapshot.utenSnapshot(Vilkårene.class));
    }

    //Denne metoden bør legges i Tjeneste
    public EndringsresultatSnapshot finnAktivBeregningsgrunnlagGrunnlagAggregatId(Long behandlingId) {
        //TODO(OJR) koble på kalkulus her
        Optional<Long> aktivBeregningsgrunnlagGrunnlagId = Optional.empty();
        return aktivBeregningsgrunnlagGrunnlagId
            .map(id -> EndringsresultatSnapshot.medSnapshot(Beregningsgrunnlag.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(Beregningsgrunnlag.class));
    }

    private Long hentLongVerdiAvEndretTid(BaseEntitet entitet) {
       LocalDateTime endretTidspunkt = entitet.getOpprettetTidspunkt();
       if(entitet.getEndretTidspunkt()!=null){
           endretTidspunkt = entitet.getEndretTidspunkt();
       }
       return mapFraLocalDateTimeTilLong(endretTidspunkt);
    }

    static Long mapFraLocalDateTimeTilLong(LocalDateTime ldt){
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("Europe/Paris"));
        return zdt.toInstant().toEpochMilli();
    }
}
